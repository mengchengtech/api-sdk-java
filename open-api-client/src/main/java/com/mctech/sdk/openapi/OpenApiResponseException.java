package com.mctech.sdk.openapi;

import lombok.Getter;

@Getter
public class OpenApiResponseException extends Exception {
    private final ApiGatewayErrorData error;

    public OpenApiResponseException(String message, ApiGatewayErrorData error) {
        super(message);

        this.error = error;
    }
}
