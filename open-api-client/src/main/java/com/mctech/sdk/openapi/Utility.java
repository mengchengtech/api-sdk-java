package com.mctech.sdk.openapi;

import com.mctech.sdk.openapi.exception.OpenApiClientException;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URIBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

class Utility
{
    private static final String OpenApiPrefix = "x-iwop-";

    private Utility() {}
    /**
     * 从map中获取以prefix的值为开头的项
     */
    private static Map<String, String> getCustomMap (Header[] headers, String prefix) {
        Map<String, String> iwopValues = new HashMap<>();
        Arrays.asList(headers).forEach(h -> {
            String lowerCaseName = h.getName().toLowerCase();
            if (lowerCaseName.startsWith(prefix)) {
                iwopValues.put(lowerCaseName, h.getValue());
            }
        });
        return iwopValues;
    }

    private static SignedData computeSignature(SignatureOption option, String time) {
        List<String> signableItems = new ArrayList<>();
        signableItems.add(option.getMethod().toUpperCase());
        if (StringUtils.isNotEmpty(option.getContentType())) {
            signableItems.add(option.getContentType());
        }
        signableItems.add(time);
        Map<String, String> custumMap = getCustomMap(option.getHeaders(), "x-iwop-");
        List<String> keys = custumMap.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        for (String key : keys) {
            signableItems.add(key + ":" + custumMap.get(key));
        }

        // add canonical resource
        String canonicalizedResource = buildCanonicalizedResource(option.getRequestUri());
        signableItems.add(canonicalizedResource);

        String signable = StringUtils.join(signableItems, "\n");
        String signature = Utility.hmacSha1(signable, option.getSecret());
        return new SignedData(signable, signature);
    }

    @SneakyThrows({NoSuchAlgorithmException.class, InvalidKeyException.class})
    private static String hmacSha1(String signable, String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        byte[] data = signable.getBytes(StandardCharsets.UTF_8);
        byte[] signedData = mac.doFinal(data);
        return Base64.encodeBase64String(signedData);
    }

    @SneakyThrows
    private static String buildCanonicalizedResource(URI requestUri)
    {
        URIBuilder urlBuilder = new URIBuilder(requestUri);
        List<NameValuePair> params = urlBuilder.getQueryParams();
        if(!params.isEmpty()) {
            params.sort(Comparator.comparing(NameValuePair::getName));
            urlBuilder.removeQuery().addParameters(params);
        }
        return urlBuilder.toString();
    }


    @SneakyThrows
    public static ApiGatewayErrorData resolveError(InputStream in) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(in);
        NodeList items = document.getDocumentElement().getChildNodes();

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < items.getLength(); i++) {
            Node item = items.item(i);
            String name = item.getNodeName();
            String value = item.getTextContent();
            map.put(name, value);
        }

        return new ApiGatewayErrorData(map);
    }

    /**
     * 生成的签名有效持续时间（秒）。默认值30秒
     * @param option
     * @return
     * @throws OpenApiClientException
     */
    public static SignedInfo generateQuerySignature(SignatureOption option) throws OpenApiClientException {
        return generateSignature(SignatureMode.QUERY, option, null);
    }

    public static SignedInfo generateQuerySignature(SignatureOption option, long duration) throws OpenApiClientException {
        return generateSignature(SignatureMode.QUERY, option, duration);
    }

    public static SignedInfo generateHeaderSignature(SignatureOption option) throws OpenApiClientException {
        return generateSignature(SignatureMode.HEADER, option, null);
    }

    private static SignedInfo generateSignature(SignatureMode mode, SignatureOption option, Long duration) throws OpenApiClientException {
        if (StringUtils.isEmpty(option.getAccessId())) {
            throw new OpenApiClientException("accessId不能为null或empty");
        }
        if (StringUtils.isEmpty(option.getSecret())) {
            throw new OpenApiClientException("secret不能为null或empty");
        }
        String method = option.getMethod().toUpperCase();
        switch (method) {
            case HttpPost.METHOD_NAME:
            case HttpPut.METHOD_NAME:
            case HttpPatch.METHOD_NAME:
                if (StringUtils.isEmpty(option.getContentType())) {
                    throw new OpenApiClientException("http请求缺少'content-type'头。请求方式为[" + method + "]时，需要在RpcInvoker的headers属性上设置'content-type'");
                }
        }
        String time;
        if (mode == SignatureMode.QUERY) {
            long d = duration != null ? duration : 30;
            long expires = d + Instant.now().getEpochSecond();
            time = Long.toString(expires);
        } else {
            time = DateUtils.formatDate(new Date(), DateUtils.PATTERN_RFC1123);
        }

        SignedData signed = Utility.computeSignature(option, time);
        Map<String, String> query = null;
        Map<String, String> headers = null;
        if (mode == SignatureMode.QUERY) {
            query = new HashMap<>();
            query.put("Signature", signed.getSignature());
            query.put("Expires", time);
        } else {
            headers = new HashMap<>();
            headers.put(HttpHeaders.AUTHORIZATION, "IWOP " + option.getAccessId() + ":" + signed.getSignature());
            headers.put(HttpHeaders.DATE, time);
        }
        return new SignedInfo(mode, signed, headers, query);
    }
}
