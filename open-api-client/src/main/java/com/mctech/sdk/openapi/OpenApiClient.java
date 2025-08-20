package com.mctech.sdk.openapi;

import com.mctech.sdk.openapi.exception.MCTechOpenApiException;
import com.mctech.sdk.openapi.exception.MCTechOpenApiRequestException;
import com.sun.istack.internal.Nullable;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

public class OpenApiClient {
    private static final String CONTENT_TYPE_VALUE = "application/json; charset=UTF-8";
    private static final String ACCEPT_VALUE = "application/json, application/xml, */*";

    private final String accessId;
    private final String secretKey;
    private final URL baseUri;
    private final CloseableHttpClient httpClient;


    @SneakyThrows
    public OpenApiClient(String baseUri, String accessId, String secretKey) {
        this.baseUri = new URL(baseUri);
        if (!StringUtils.isNotEmpty(accessId)) {
            throw new MCTechOpenApiException("accessId不能为null或empty");
        }
        if (!StringUtils.isNotEmpty(secretKey)) {
            throw new MCTechOpenApiException("secret不能为null或empty");
        }

        this.accessId = accessId;
        this.secretKey = secretKey;
        this.httpClient = HttpClients.createDefault();
    }

    public RequestResult get(String apiPath, RequestOption option)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        return this.request(HttpGet.class, apiPath, option);
    }

    public RequestResult delete(String apiPath, RequestOption option)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        return this.request(HttpDelete.class, apiPath, option);
    }

    public RequestResult post(String apiPath, RequestOption option)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        return this.request(HttpPost.class, apiPath, option);
    }

    public RequestResult put(String apiPath, RequestOption option)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        return this.request(HttpPut.class, apiPath, option);
    }

    public RequestResult patch(String apiPath, RequestOption option)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        return this.request(HttpPatch.class, apiPath, option);
    }

    @SneakyThrows({InstantiationException.class, IllegalAccessException.class})
    private <T extends HttpRequestBase> RequestResult request(Class<T> cls, String apiPath, RequestOption option)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        T req = cls.newInstance();
        return this.request(req, apiPath, option);
    }

    @SneakyThrows({IOException.class, URISyntaxException.class})
    public RequestResult request(HttpRequestBase req, String apiPath, RequestOption option)
        throws MCTechOpenApiException, MCTechOpenApiRequestException {
        if (req instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = option.getEntity();
            if (entity != null) {
                ((HttpEntityEnclosingRequest) req).setEntity(entity);
            }
        }

        StringBuilder qs = new StringBuilder("?");
        Map<String, String> query = option.getQuery();
        if (query != null && !query.isEmpty()) {
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (qs.length() > 1) {
                    qs.append("&");
                }
                String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name());
                String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name());
                qs.append(key).append("=").append(value);
            }
        }
        String pathAndQuery = qs.length() > 1 ? apiPath + qs : apiPath;
        URL apiUrl = new URL(this.baseUri, pathAndQuery);
        req.setURI(apiUrl.toURI());

        String formatDate = DateUtils.formatDate(new Date());
        req.setHeader(new BasicHeader(HttpHeaders.DATE, formatDate));
        req.setHeader(new BasicHeader(HttpHeaders.ACCEPT, ACCEPT_VALUE));
        req.setHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"));
        req.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_VALUE));
        makeSignature(req, option.getHeaders());
        CloseableHttpResponse response = this.httpClient.execute(req);
        return new RequestResult(response);
    }

    @SneakyThrows({NoSuchAlgorithmException.class, InvalidKeyException.class})
    private void makeSignature(HttpUriRequest req, @Nullable Map<String, String> headers)
            throws MCTechOpenApiException {
        SignatureOption option = new SignatureOption(req.getURI(), req.getMethod(), CONTENT_TYPE_VALUE,
                req.getFirstHeader(HttpHeaders.DATE).getValue());
        if (headers != null) {
            headers.forEach(req::setHeader);
            headers.forEach(option.getHeaders()::put);
        }
        String canonicalString = SignUtility.buildCanonicalString(option);
        byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        byte[] data = canonicalString.getBytes(StandardCharsets.UTF_8);
        byte[] signedData = mac.doFinal(data);
        String signature = Base64.encodeBase64String(signedData);
        req.addHeader(HttpHeaders.AUTHORIZATION, "IWOP " + this.accessId + ":" + signature);
    }
}
