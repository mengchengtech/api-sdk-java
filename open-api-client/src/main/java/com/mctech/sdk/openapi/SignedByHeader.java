package com.mctech.sdk.openapi;

public class SignedByHeader implements SignedBy {
    @Override
    public SignatureMode getMode() {
        return SignatureMode.HEADER;
    }
}
