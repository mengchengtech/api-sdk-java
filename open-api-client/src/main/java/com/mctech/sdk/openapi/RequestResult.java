package com.mctech.sdk.openapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.mctech.sdk.openapi.exception.MCTechOpenApiRequestException;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestResult implements Closeable {

    private final CloseableHttpResponse response;

    /**
     * 返回结果状态码
     */
    @Getter
    private final int statusCode;

    /**
     * 获取内容的ContentType
     */
    @Getter
    private String contentType;

    /**
     * 以字符串方式获取返回的文本内容
     */
    public String getContent() throws IOException {
        HttpEntity entity = this.response.getEntity();
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    public JSON getJsonObject() throws IOException {
        HttpEntity entity = this.response.getEntity();
        InputStream in = entity.getContent();
        return JSON.parseObject(in, JSON.class);
    }

    public <T> T getObject(Class<T> cls) throws IOException {
        HttpEntity entity = this.response.getEntity();
        InputStream in = entity.getContent();
        return JSON.parseObject(in, cls);
    }

    public <T> List<T> getList(Class<T> cls) throws IOException {
        HttpEntity entity = this.response.getEntity();
        InputStream in = entity.getContent();
        JSONArray array = JSON.parseObject(in, JSONArray.class);
        return array.toJavaList(cls);
    }

    /**
     * @return 获取一个用于读返回结果的流
     */
    public InputStream openRead() throws IOException {
        HttpEntity entity = this.response.getEntity();
        return entity.getContent();
    }

    RequestResult(CloseableHttpResponse response) throws MCTechOpenApiRequestException {
        this.response = response;
        Header h = response.getEntity().getContentType();
        if (h != null) {
            contentType = h.getValue();
        }

        this.statusCode = response.getStatusLine().getStatusCode();

        if (this.statusCode >= HttpStatus.SC_BAD_REQUEST) {
            ApiGatewayError error = createError(response);
            throw new MCTechOpenApiRequestException(error.getMessage(), error);
        }
    }

    public void close() throws IOException {
        this.response.close();
    }

    @SneakyThrows
    private static ApiGatewayError createError(CloseableHttpResponse response) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(response.getEntity().getContent());
        NodeList items = document.getDocumentElement().getChildNodes();

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < items.getLength(); i++) {
            Node item = items.item(i);
            String name = item.getNodeName();
            String value = item.getTextContent();
            map.put(name, value);
        }

        return new ApiGatewayError(map);
    }
}

