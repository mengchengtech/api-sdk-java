package com.mctech.sdk.openapi;

import com.sun.istack.internal.Nullable;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

public class OpenApiClient {
    private static final String CONTENT_TYPE_VALUE = "application/json; charset=UTF-8";
    private static final String ACCEPT_VALUE = "application/json, application/xml, */*";

    private final String accessId;
    private final String secretKey;
    private final URL baseUri;
    private final CloseableHttpClient httpClient;


    @SneakyThrows({MalformedURLException.class})
    public OpenApiClient(String baseUri, String accessId, String secretKey) {
        this.baseUri = new URL(baseUri);
        if (!StringUtils.isNotEmpty(accessId)) {
            throw new OpenApiClientException("accessId不能为null或empty");
        }
        if (!StringUtils.isNotEmpty(secretKey)) {
            throw new OpenApiClientException("secret不能为null或empty");
        }

        this.accessId = accessId;
        this.secretKey = secretKey;
        this.httpClient = HttpClients.custom()
                .setDefaultHeaders(
                        Arrays.asList(
                                new BasicHeader(HttpHeaders.ACCEPT, ACCEPT_VALUE),
                                // new BasicHeader(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate"), // default enable "gzip,deflate"
                                new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN")
                        )
                )
                .build();
    }

    public RequestResult get(String apiPath, RequestOption option) throws OpenApiResponseException {
        return this.request(new HttpGet(), apiPath, option);
    }

    public RequestResult delete(String apiPath, RequestOption option) throws OpenApiResponseException {
        return this.request(new HttpDelete(), apiPath, option);
    }

    public RequestResult post(String apiPath, RequestOption option) throws OpenApiResponseException {
        return this.request(new HttpPost(), apiPath, option);
    }

    public RequestResult put(String apiPath, RequestOption option) throws OpenApiResponseException {
        return this.request(new HttpPut(), apiPath, option);
    }

    public RequestResult patch(String apiPath, RequestOption option) throws OpenApiResponseException {
        return this.request(new HttpPatch(), apiPath, option);
    }

    @SneakyThrows({IOException.class, URISyntaxException.class})
    public RequestResult request(HttpRequestBase req, String apiPath, RequestOption option)
            throws OpenApiResponseException {
        if (req instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = option.getEntity();
            HttpEntityEnclosingRequest entityReq = (HttpEntityEnclosingRequest) req;
            if (entity != null) {
                entityReq.setEntity(entity);
            }
            if (entityReq.getEntity() != null) {
                String contentType = option.getContentType();
                if (StringUtils.isEmpty(option.getContentType())) {
                    contentType = CONTENT_TYPE_VALUE;
                }
                req.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
            }
        }
        URI apiUri = new URL(this.baseUri, apiPath).toURI();
        if (!option.getQuery().isEmpty()) {
            URIBuilder urlBuilder = new URIBuilder(apiUri);
            option.getQuery().forEach(urlBuilder::setParameter);
            apiUri = urlBuilder.build();
        }
        req.setURI(apiUri);
        if (!option.getHeaders().isEmpty()) {
            option.getHeaders().forEach(req::setHeader);
        }

        this.makeSignature(req, option.getSignedBy());

        Integer timeout = option.getTimeout();
        if (timeout != null && timeout > 0) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(timeout)            // 响应数据传输超时
                    .build();
            req.setConfig(requestConfig);
        }
        CloseableHttpResponse response = this.httpClient.execute(req);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= HttpStatus.SC_BAD_REQUEST) {
            InputStream xmlContent = response.getEntity().getContent();
            ApiGatewayErrorData error = Utility.resolveError(xmlContent);
            throw new OpenApiResponseException(error.getMessage(), statusCode, error);
        }
        return new RequestResult(response);
    }

    @SneakyThrows({URISyntaxException.class})
    private void makeSignature(HttpRequestBase req, @Nullable SignedBy signedBy) {
        NameValuePair contentType = req.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        SignatureOption option = new SignatureOption(
                this.accessId,
                this.secretKey,
                req.getURI(),
                req.getMethod(),
                contentType != null ? contentType.getValue() : null,
                Arrays.asList(req.getAllHeaders())
        );

        if (signedBy == null) {
            signedBy = new SignedByHeader();
        }
        SignedInfo signedInfo = Utility.generateSignature(signedBy, option);
        if (signedInfo.getHeaders() != null) {
            signedInfo.getHeaders().forEach(req::setHeader);
        }

        if (signedInfo.getQuery() != null) {
            URIBuilder builder = new URIBuilder(req.getURI());
            signedInfo.getQuery().forEach(builder::setParameter);
            URI targetURI = builder.build();
            req.setURI(targetURI);
        }
    }
}
