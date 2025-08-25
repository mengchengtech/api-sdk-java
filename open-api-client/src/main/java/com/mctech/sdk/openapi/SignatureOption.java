package com.mctech.sdk.openapi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.Header;

import java.net.URI;

@Getter
@AllArgsConstructor
class SignatureOption
{
    private final String accessId;
    private final String secret;
    private final URI requestUri;
    private final String method;
    private final String contentType;
    private final Header[] headers;
}
