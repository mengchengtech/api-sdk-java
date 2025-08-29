package com.mctech.sdk.sample;


import com.alibaba.fastjson.JSONObject;
import com.mctech.sdk.openapi.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class ApiClientTest {
    private static Config config;
    private static OpenApiClient client;

    @BeforeAll
    @SneakyThrows
    static void init() {
        config = new Config();
        try (InputStream in = ApiClientTest.class.getResourceAsStream("/app.properties")) {
            config.load(in);
        }
        client = new OpenApiClient(config.getBaseUrl(), config.getAccessId(), config.getSecretKey());
    }

    @Test
    @SneakyThrows
    void testGetByHeaderAndReturnResult() {
        RequestOption option = RequestOption.newBuilder()
                .addQuery("integratedProjectId", config.getIntegrationId())
                .addHeader(new HashMap<String, Object>() {{
                    put("X-iwop-before", "wq666");
                    put("x-iwop-integration-id", config.getIntegrationId());
                    put("x-IWOP-after", "wq666");
                }})
                .build();
        RequestResult result = client.get(config.getApiPath(), option);
        assertEquals(200, result.getStatus());
        assertEquals("application/json", result.getContentType().split(";")[0]);
        JSONObject data = (JSONObject) result.getJsonObject();
        assertTrue(data.containsKey("updateAt"));
        assertTrue(data.containsKey("data"));
    }

    @Test
    @SneakyThrows
    void testGetByQueryAndApiNotExists() {
        RequestOption option = RequestOption.newBuilder()
                .signedBy(new SignedByQuery(new QuerySignatureParams(3600)))
                .addQuery("integratedProjectId", config.getIntegrationId())
                .addQuery(new HashMap<String, Object>() {{
                    put("X-iwop-before", "wq666");
                    put("x-iwop-integration-id", config.getIntegrationId());
                    put("x-IWOP-after", "wq666");
                }})
                .build();
        RequestResult result = client.get(config.getApiPath(), option);
        assertEquals(200, result.getStatus());
        assertEquals("application/json", result.getContentType().split(";")[0]);
        JSONObject data = (JSONObject) result.getJsonObject();
        assertTrue(data.containsKey("updateAt"));
        assertTrue(data.containsKey("data"));
    }

    @Test
    @SneakyThrows
    void testPostByHeaderAndApiNotExists() {
        RequestOption option = RequestOption.newBuilder()
                .addQuery("integratedProjectId", config.getIntegrationId())
                .addHeader("x-iwop-integration-id", config.getIntegrationId())
                .addHeader("x-forwarded-for", "192.168.1.1")
                .contentType("application/xml")
                .body("<body></body>")
                .build();
        try (RequestResult result = this.client.post(config.getApiPath(), option)) {
            fail();
        } catch (OpenApiResponseException e) {
            assertEquals(404, e.getStatus());
            assertEquals("192.168.1.1", e.getError().getClientIP());
            assertEquals("SERVICE_NOT_FOUND", e.getError().getCode());
            assertEquals("'POST /api-ex/-itg-/cb/project-wbs/items' 对应的服务不存在。请检查rest请求中的method, path是否与相应api文档中的完全一致",
                    e.getError().getMessage());
        }
    }
}
