package com.mctech.sdk.openapi;

import com.mctech.sdk.openapi.exception.MCTechOpenApiException;
import com.mctech.sdk.openapi.exception.MCTechOpenApiRequestException;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class OpenApiClient {
    private static final String DEFAULT_CHARSET_NAME = "utf-8";
    private static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);

    private static final String CONTENT_TYPE = "application/json; charset=UTF-8";
    private static final String ACCEPT = "application/json, application/xml";

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

    public RequestResult get(String pathAndQuery)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        HttpGet httpGet = new HttpGet();
        return SendRequest(pathAndQuery, httpGet);
    }

    public RequestResult delete(String pathAndQuery)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        HttpDelete httpDelete = new HttpDelete();
        return SendRequest(pathAndQuery, httpDelete);
    }

    public RequestResult post(String pathAndQuery, String body)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        HttpPost httpPost = new HttpPost();
        byte[] data = body.getBytes(DEFAULT_CHARSET);
        httpPost.setEntity(new ByteArrayEntity(data));
        return SendRequest(pathAndQuery, httpPost);
    }

    public RequestResult post(String pathAndQuery, InputStream streamBody)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        HttpPost httpPost = new HttpPost();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(streamBody);
        httpPost.setEntity(entity);
        return SendRequest(pathAndQuery, httpPost);
    }

    public RequestResult put(String pathAndQuery, String body)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        HttpPut httpPut = new HttpPut();
        byte[] data = body.getBytes(Charset.forName("utf-8"));
        httpPut.setEntity(new ByteArrayEntity(data));
        return SendRequest(pathAndQuery, httpPut);
    }

    public RequestResult put(String pathAndQuery, InputStream streamBody)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        HttpPut httpPut = new HttpPut();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(streamBody);
        httpPut.setEntity(entity);
        return SendRequest(pathAndQuery, httpPut);
    }

    public RequestResult patch(String pathAndQuery, String body)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        HttpPatch httpPatch = new HttpPatch();
        byte[] data = body.getBytes(Charset.forName("utf-8"));
        httpPatch.setEntity(new ByteArrayEntity(data));
        return SendRequest(pathAndQuery, httpPatch);
    }

    public RequestResult patch(String pathAndQuery, InputStream streamBody)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        HttpPatch httpPatch = new HttpPatch();
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(streamBody);
        httpPatch.setEntity(entity);
        return SendRequest(pathAndQuery, httpPatch);
    }

    @SneakyThrows({ IOException.class, URISyntaxException.class })
    private RequestResult SendRequest(String pathAndQuery, HttpRequestBase request)
            throws MCTechOpenApiException, MCTechOpenApiRequestException {
        URL apiUrl = new URL(this.baseUri, pathAndQuery);
        request.setURI(apiUrl.toURI());
        initRequest(request);
        CloseableHttpResponse response = this.httpClient.execute(request);
        RequestResult result = new RequestResult(response);
        return result;
    }

    @SneakyThrows({ NoSuchAlgorithmException.class, InvalidKeyException.class })
    private void initRequest(HttpUriRequest request)
            throws MCTechOpenApiException {
        String formatDate = DateUtils.formatDate(new Date());
        request.setHeader(new BasicHeader(HttpHeaders.DATE, formatDate));
        request.setHeader(new BasicHeader(HttpHeaders.ACCEPT, ACCEPT));
        request.setHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"));
        request.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE));

        String httpMethod = request.getMethod();
        SignatureOption option = new SignatureOption(request.getURI(), httpMethod, CONTENT_TYPE, formatDate);
        String canonicalString = SignUtility.buildCanonicalString(option);

        byte[] key = secretKey.getBytes(DEFAULT_CHARSET);
        SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        byte[] data = canonicalString.getBytes(DEFAULT_CHARSET);
        byte[] signedData = mac.doFinal(data);
        String signature = Base64.encodeBase64String(signedData);
        request.addHeader(HttpHeaders.AUTHORIZATION, "IWOP " + this.accessId + ":" + signature);
    }
}
