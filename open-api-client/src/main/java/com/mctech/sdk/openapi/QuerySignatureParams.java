package com.mctech.sdk.openapi;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuerySignatureParams {
    /**
     * 生成的签名有效持续时间（秒）。默认值30秒
     */
    private int duration;
}
