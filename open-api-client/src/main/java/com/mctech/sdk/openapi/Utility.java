package com.mctech.sdk.openapi;

import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
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

import static com.mctech.sdk.openapi.Constants.*;

class Utility {
    private Utility() {
    }

    /**
     * 从map中获取以prefix的值为开头的项
     */
    private static Map<String, String> getCustomMap(List<? extends NameValuePair> pairs) {
        Map<String, String> iwopValues = new HashMap<>();
        pairs.forEach(h -> {
            String lowerCaseName = h.getName().toLowerCase();
            if (lowerCaseName.startsWith(CUSTOM_PREFIX)) {
                iwopValues.put(lowerCaseName, h.getValue());
            }
        });
        return iwopValues;
    }

    @SneakyThrows
    private static String getResource(URI requestUri) {
        URIBuilder urlBuilder = new URIBuilder(requestUri);
        if (!urlBuilder.isQueryEmpty()) {
            List<NameValuePair> params = urlBuilder.getQueryParams().stream()
                    .filter(p -> {
                        // 排除掉表用于认证的固定参数
                        if (QUERY_KEYS.contains(p.getName())) {
                            return false;
                        }
                        // 排除掉特定前缀的参数，例如 'x-iwop-'
                        return !p.getName().toLowerCase().startsWith(CUSTOM_PREFIX);
                    })
                    .sorted(Comparator.comparing(NameValuePair::getName))
                    .collect(Collectors.toList());
            urlBuilder.removeQuery().addParameters(params);
        }
        return urlBuilder.toString();
    }

    private static SignedData computeSignature(SignatureMode mode, SignatureOption option, String time) {
        List<String> signableItems = new ArrayList<>();
        signableItems.add(option.getMethod().toUpperCase());
        if (StringUtils.isNotEmpty(option.getContentType())) {
            signableItems.add(option.getContentType());
        }
        signableItems.add(time);
        Map<String, String> customMap;
        if (mode == SignatureMode.HEADER) {
            customMap = getCustomMap(option.getHeaders());
        } else if (mode == SignatureMode.QUERY) {
            List<? extends NameValuePair> params = new URIBuilder(option.getRequestUri()).getQueryParams();
            customMap = getCustomMap(params);
        } else {
            customMap = null;
        }
        if (customMap != null) {
            customMap.keySet().stream()
                    .sorted()
                    .forEach(key -> {
                        String item = String.format("%s:%s", key, customMap.get(key));
                        signableItems.add(item);
                    });
        }

        String canonicalizedResource = getResource(option.getRequestUri());
        signableItems.add(canonicalizedResource);

        String signable = StringUtils.join(signableItems, "\n");
        String signature = hmacSha1(signable, option.getSecret());
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

    public static SignedInfo generateSignature(SignedBy signedBy, SignatureOption option) {
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
        Map<String, String> query = null;
        Map<String, String> headers = null;
        if (signedBy.getMode() == SignatureMode.QUERY) {
            QuerySignatureParams p = ((SignedByQuery) signedBy).getParameters();
            long d = p != null && p.getDuration() > 0 ? p.getDuration() : DEFAULT_EXPIRES;
            long expires = d + Instant.now().getEpochSecond();
            time = Long.toString(expires);
            query = new HashMap<String, String>() {{
                put(QUERY_ACCESS_ID, option.getAccessId());
                put(QUERY_EXPIRES, time);
                put(QUERY_SIGNATURE, null);
            }};
        } else {
            time = DateUtils.formatDate(new Date(), DateUtils.PATTERN_RFC1123);
            headers = new HashMap<String, String>() {{
                put(HttpHeaders.DATE, time);
                put(HttpHeaders.AUTHORIZATION, null);
            }};
        }

        SignedData signed = computeSignature(signedBy.getMode(), option, time);
        if (query != null) {
            query.put(QUERY_SIGNATURE, signed.getSignature());
        } else {
            headers.put(HttpHeaders.AUTHORIZATION, "IWOP " + option.getAccessId() + ":" + signed.getSignature());
        }
        return new SignedInfo(signedBy.getMode(), signed, headers, query);
    }
}
