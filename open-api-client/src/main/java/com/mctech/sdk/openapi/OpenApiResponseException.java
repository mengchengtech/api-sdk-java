package com.mctech.sdk.openapi;

import lombok.Getter;

@Getter
public class OpenApiResponseException extends Exception {
    private final ApiGatewayErrorData error;
    private final int status;

    public OpenApiResponseException(String message, int status, ApiGatewayErrorData error) {
        this(message, status, error, null);
    }

    public OpenApiResponseException(String message, int status, ApiGatewayErrorData error, Throwable cause) {
        super(message, cause);

        this.error = error;
        this.status = status;
    }
}
