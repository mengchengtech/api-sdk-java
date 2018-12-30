package com.mctech.sdk.openapi;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Getter
class SignatureOption
{
    private final URI requestUri;
    private final String method;
    private final String contentType;
    private final String formatedDate;
    //private readonly string _contentMd5;
    private final Map<String, String> headers;

    public SignatureOption(URI requestUri, String method, String contentType, String formatedDate)
    {
        if (method == HttpMethod.Post || method == HttpMethod.Put || method == HttpMethod.Patch) {
            if (!StringUtils.isNotEmpty(contentType)) {
                throw new MctechOpenApiException("http请求缺少'content-type'头。请求方式为[" + method + "]时，需要在RpcInvoker的headers属性上设置'content-type'");
            }
        }

        this.requestUri = requestUri;
        this.method = method;
        this.contentType = contentType;
        //this._contentMd5 = "";
        this.formatedDate = formatedDate;
        this.headers = new HashMap<>();
    }
}
