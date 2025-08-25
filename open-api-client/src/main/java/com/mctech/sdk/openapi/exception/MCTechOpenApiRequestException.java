package com.mctech.sdk.openapi.exception;

import com.mctech.sdk.openapi.ApiGatewayError;
import lombok.Getter;

public class MCTechOpenApiRequestException extends MCTechException {
    @Getter
    private final ApiGatewayError error;

    public MCTechOpenApiRequestException(String message, ApiGatewayError error) {
        super(message);

        this.error = error;
    }
}
