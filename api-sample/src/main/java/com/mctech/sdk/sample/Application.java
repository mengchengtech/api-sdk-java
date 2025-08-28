package com.mctech.sdk.sample;

import com.alibaba.fastjson.JSON;
import com.mctech.sdk.openapi.*;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class Application {
    private static final String accessId = "{accessId}";
    private static final String secretKey = "{secretKey}";

    private static final String baseUrl = "https://test.mctech.vip/api-ex/-itg-/";

    private static final String apiPath = "cb/project-wbs/items";

    private static final String integrationId = "{integrationId}";

    private static final Logger logger = LogManager.getLogger("logger");

    private static final OpenApiClient client = new OpenApiClient(baseUrl, accessId, secretKey);

    public static void main(String[] args) {
        testGetByHeader();
        testGetByQuery();
        testPostByHeader();
    }

    @SneakyThrows
    private static void testGetByHeader() {
        RequestOption option = RequestOption.newBuilder()
                .addQuery("integratedProjectId", integrationId)
                .addHeaders(new HashMap<String, Object>() {{
                    put("X-iwop-before", "wq666");
                    put("x-iwop-integration-id", integrationId);
                    put("x-IWOP-after", "wq666");
                }})
                .build();
        try (RequestResult result = client.get(apiPath, option)) {
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
    private static void testGetByQuery() {
        RequestOption option = RequestOption.newBuilder()
                .signedBy(new SignedByQuery(new QuerySignatureParams(3600)))
                .addQuery("integratedProjectId", integrationId)
                .addQueries(new HashMap<String, Object>() {{
                    put("X-iwop-before", "wq666");
                    put("x-iwop-integration-id", integrationId);
                    put("x-IWOP-after", "wq666");
                }})
                .build();
        try (RequestResult result = client.get(apiPath, option)) {
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
    private static void testPostByHeader() {
        RequestOption option = RequestOption.newBuilder()
                .addQuery("integratedProjectId", integrationId)
                .addHeader("x-iwop-integration-id", integrationId)
                .contentType("application/xml")
                .body("<body></body>")
                .build();
        try (RequestResult result = client.post(apiPath, option)) {
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
