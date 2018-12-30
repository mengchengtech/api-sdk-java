package com.mctech.sdk.openapi;

import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class OpenApiClient {
    private final String accessId;
    private final String secretKey;
    private final URL baseUri;
    private final CloseableHttpClient httpClient;


    @SneakyThrows
    public OpenApiClient(String baseUri, String accessId, String secretKey) {
        this.baseUri = new URL(baseUri);
        if (!StringUtils.isNotEmpty(accessId)) {
            throw new MctechOpenApiException("accessId不能为null或empty");
        }
        if (!StringUtils.isNotEmpty(secretKey)) {
            throw new MctechOpenApiException("secret不能为null或empty");
        }

        this.accessId = accessId;
        this.secretKey = secretKey;
        this.httpClient = HttpClients.createDefault();
    }

    @SneakyThrows
    public RequestResult get(String pathAndQuery) {
        HttpGet httpGet = new HttpGet();
        return SendRequest(pathAndQuery, httpGet);
    }

    public RequestResult delete(String pathAndQuery) {
        HttpDelete httpDelete = new HttpDelete();
        return SendRequest(pathAndQuery, httpDelete);
    }

    public RequestResult post(String pathAndQuery, String body) {
        HttpPost httpPost = new HttpPost();
        byte[] data = body.getBytes(Charset.forName("utf-8"));
        httpPost.setEntity(new ByteArrayEntity(data));
        return SendRequest(pathAndQuery, httpPost);
    }

    public RequestResult post(String pathAndQuery, InputStream streamBody) {
        HttpPost httpPost = new HttpPost();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(streamBody);
        httpPost.setEntity(entity);
        return SendRequest(pathAndQuery, httpPost);
    }

    public RequestResult put(String pathAndQuery, String body) {
        HttpPut httpPut = new HttpPut();
        byte[] data = body.getBytes(Charset.forName("utf-8"));
        httpPut.setEntity(new ByteArrayEntity(data));
        return SendRequest(pathAndQuery, httpPut);
    }

    public RequestResult put(String pathAndQuery, InputStream streamBody) {
        HttpPut httpPut = new HttpPut();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(streamBody);
        httpPut.setEntity(entity);
        return SendRequest(pathAndQuery, httpPut);
    }

    public RequestResult patch(String pathAndQuery, String body) {
        HttpPatch httpPatch = new HttpPatch();
        byte[] data = body.getBytes(Charset.forName("utf-8"));
        httpPatch.setEntity(new ByteArrayEntity(data));
        return SendRequest(pathAndQuery, httpPatch);
    }

    public RequestResult patch(String pathAndQuery, InputStream streamBody) {
        HttpPatch httpPatch = new HttpPatch();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(streamBody);
        httpPatch.setEntity(entity);
        return SendRequest(pathAndQuery, httpPatch);
    }

    @SneakyThrows
    private RequestResult SendRequest(String pathAndQuery, HttpRequestBase request) {
        URL apiUrl = new URL(this.baseUri, pathAndQuery);
        request.setURI(apiUrl.toURI());
        initRequest(request);
        CloseableHttpResponse response = this.httpClient.execute(request);
        RequestResult result = new RequestResult(response);
        return result;
    }

    private static final String CONTENT_TYPE = "application/json, application/xml";

    private void initRequest(HttpUriRequest request)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String formatDate = DateUtils.formatDate(new Date());
        request.setHeader(new BasicHeader(HttpHeaders.DATE, formatDate));
        request.setHeader(new BasicHeader(HttpHeaders.ACCEPT, formatDate));
        request.setHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"));
        request.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE));
        // request.KeepAlive = true;
        // request.Headers.Add(HttpRequestHeader.KeepAlive, "3000");

        String httpMethod = request.getMethod();
        SignatureOption option = new SignatureOption(request.getURI(), httpMethod, CONTENT_TYPE, formatDate);
        String canonicalString = SignUtility.buildCanonicalString(option);

        byte[] key = secretKey.getBytes("utf-8");
        SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        byte[] data = canonicalString.getBytes("utf-8");
        byte[] signedData = mac.doFinal(data);
        String signature = Base64.encodeBase64String(signedData);
        request.addHeader(HttpHeaders.AUTHORIZATION, "IWOP " + this.accessId + ":" + signature);
    }
}
