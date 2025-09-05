package com.mctech.sdk.openapi;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignedByQuery implements SignedBy {
    private QuerySignatureParams parameters;

    @Override
    public SignatureMode getMode() {
        return SignatureMode.QUERY;
    }
}
