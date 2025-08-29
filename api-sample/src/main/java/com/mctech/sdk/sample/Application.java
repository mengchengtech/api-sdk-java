package com.mctech.sdk.sample;

import com.alibaba.fastjson.JSON;
import com.mctech.sdk.openapi.*;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.HashMap;

public class Application {

    private static final Logger logger = LogManager.getLogger("logger");

    private final Config config;
    private final OpenApiClient client;

    @SneakyThrows
    public Application() {
        this.config = new Config();
        try (InputStream in = Application.class.getResourceAsStream("/app.properties")) {
            config.load(in);
        }
        client = new OpenApiClient(config.getBaseUrl(), config.getAccessId(), config.getSecretKey());
    }

    public static void main(String[] args) {
        Application app = new Application();
        app.testGetByHeader();
        app.testGetByQuery();
        app.testPostByHeader();
    }

    @SneakyThrows
    private void testGetByHeader() {
        RequestOption option = RequestOption.newBuilder()
                .addQuery("integratedProjectId", config.getIntegrationId())
                .addHeader(new HashMap<String, Object>() {{
                    put("X-iwop-before", "wq666");
                    put("x-iwop-integration-id", config.getIntegrationId());
                    put("x-IWOP-after", "wq666");
                }})
                .build();
        try (RequestResult result = client.get(config.getApiPath(), option)) {
            System.out.println(result.getString());
        } catch (OpenApiClientException e) {
            logger.error(e.getMessage(), e);
            // TODO: 处理异常
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        } catch (OpenApiResponseException e) {
            logger.error(e.getMessage(), e);
            ApiGatewayErrorData error = e.getError();
            // TODO: 处理api网关返回的异常
            logger.error(JSON.toJSON(error));
        }
    }

    @SneakyThrows
    private void testGetByQuery() {
        RequestOption option = RequestOption.newBuilder()
                .signedBy(new SignedByQuery(new QuerySignatureParams(3600)))
                .addQuery("integratedProjectId", config.getIntegrationId())
                .addQuery(new HashMap<String, Object>() {{
                    put("X-iwop-before", "wq666");
                    put("x-iwop-integration-id", config.getIntegrationId());
                    put("x-IWOP-after", "wq666");
                }})
                .build();
        try (RequestResult result = this.client.get(config.getApiPath(), option)) {
            System.out.println(result.getString());
        } catch (OpenApiClientException e) {
            logger.error(e.getMessage(), e);
            // TODO: 处理异常
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        } catch (OpenApiResponseException e) {
            logger.error(e.getMessage(), e);
            ApiGatewayErrorData error = e.getError();
            // TODO: 处理api网关返回的异常
            logger.error(JSON.toJSON(error));
        }
    }

    @SneakyThrows
    private void testPostByHeader() {
        RequestOption option = RequestOption.newBuilder()
                .addQuery("integratedProjectId", config.getIntegrationId())
                .addHeader("x-iwop-integration-id", config.getIntegrationId())
                .contentType("application/xml")
                .body("<body></body>")
                .build();
        try (RequestResult result = this.client.post(config.getApiPath(), option)) {
            System.out.println(result.getString());
        } catch (OpenApiClientException e) {
            logger.error(e.getMessage(), e);
            // TODO: 处理异常
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        } catch (OpenApiResponseException e) {
            logger.error(e.getMessage(), e);
            ApiGatewayErrorData error = e.getError();
            // TODO: 处理api网关返回的异常
            logger.error(JSON.toJSON(error));
        }
    }
}
