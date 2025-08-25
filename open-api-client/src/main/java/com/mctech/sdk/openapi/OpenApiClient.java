package com.mctech.sdk.openapi;

import com.mctech.sdk.openapi.exception.OpenApiClientException;
import com.mctech.sdk.openapi.exception.OpenApiRequestException;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
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
                            new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
                    )
            .build();
    }

    public RequestResult get(String apiPath, RequestOption option)
            throws OpenApiClientException, OpenApiRequestException {
        return this.request(HttpGet.class, apiPath, option);
    }

    public RequestResult delete(String apiPath, RequestOption option)
            throws OpenApiClientException, OpenApiRequestException {
        return this.request(HttpDelete.class, apiPath, option);
    }

    public RequestResult post(String apiPath, RequestOption option)
            throws OpenApiClientException, OpenApiRequestException {
        return this.request(HttpPost.class, apiPath, option);
    }

    public RequestResult put(String apiPath, RequestOption option)
            throws OpenApiClientException, OpenApiRequestException {
        return this.request(HttpPut.class, apiPath, option);
    }

    public RequestResult patch(String apiPath, RequestOption option)
            throws OpenApiClientException, OpenApiRequestException {
        return this.request(HttpPatch.class, apiPath, option);
    }

    @SneakyThrows({InstantiationException.class, IllegalAccessException.class})
    private <T extends HttpRequestBase> RequestResult request(Class<T> cls, String apiPath, RequestOption option)
            throws OpenApiClientException, OpenApiRequestException {
        T req = cls.newInstance();
        return this.request(req, apiPath, option);
    }

    @SneakyThrows({IOException.class, URISyntaxException.class})
    public RequestResult request(HttpRequestBase req, String apiPath, RequestOption option)
        throws OpenApiClientException, OpenApiRequestException {
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
        if (option.getQuery() != null) {
            URIBuilder urlBuilder = new URIBuilder(apiUri);
            option.getQuery().forEach(urlBuilder::addParameter);
            apiUri = urlBuilder.build();
        }
        req.setURI(apiUri);
        makeSignature(req, option.getHeaders());

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
            throw new OpenApiRequestException(error.getMessage(), error);
        }

        return new RequestResult(response);
    }

    private void makeSignature(HttpUriRequest req, @Nullable Map<String, String> headers)
            throws OpenApiClientException {
        if (headers != null) {
            headers.forEach(req::setHeader);
        }
        Header contentType = req.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        SignatureOption option = new SignatureOption(
                this.accessId,
                this.secretKey,
                req.getURI(),
                req.getMethod(),
                contentType != null ? contentType.getValue(): null,
                req.getAllHeaders()
        );

        SignedInfo signedInfo = Utility.generateHeaderSignature(option);
        signedInfo.getHeaders().forEach(req::setHeader);
    }
}
