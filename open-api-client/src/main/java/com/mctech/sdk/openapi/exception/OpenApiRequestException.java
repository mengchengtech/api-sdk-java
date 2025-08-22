package com.mctech.sdk.openapi.exception;

import com.mctech.sdk.openapi.ApiGatewayErrorData;
import lombok.Getter;

public class OpenApiRequestException extends OpenApiException {
    @Getter
    private final ApiGatewayErrorData error;

    public OpenApiRequestException(String message, ApiGatewayErrorData error) {
        super(message);

        this.error = error;
    }
}
