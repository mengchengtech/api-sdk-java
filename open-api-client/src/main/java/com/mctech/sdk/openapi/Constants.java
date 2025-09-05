package com.mctech.sdk.openapi;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
    public final static String QUERY_ACCESS_ID = "AccessId";
    public final static String QUERY_EXPIRES = "Expires";
    public final static String QUERY_SIGNATURE = "Signature";

    public final static List<String> QUERY_KEYS = Arrays.asList("AccessId", "Signature", "Expires");
    public final static String CUSTOM_PREFIX = "x-iwop-";
    /**
     * 生成Query签名时间有效期默认值，单位秒
     */
    public final static long DEFAULT_EXPIRES = 30;
}
