package com.mctech.sdk.openapi;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignedData {
    private final String signable;
    private final String signature;
}
