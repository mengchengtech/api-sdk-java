package com.mctech.sdk.openapi;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class SignedInfo {
    private final SignatureMode mode;
    private final SignedData signed;
    private final Map<String, String> headers;
    private final Map<String, String> query;
}
