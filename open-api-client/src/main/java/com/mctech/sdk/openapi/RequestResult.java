package com.mctech.sdk.openapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.Getter;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.*;
import java.util.List;

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

    RequestResult(CloseableHttpResponse response) {
        this.response = response;
        Header h = response.getEntity().getContentType();
        if (h != null) {
            this.contentType = h.getValue();
        }
        this.statusCode = response.getStatusLine().getStatusCode();
    }

    public void close() throws IOException {
        this.response.close();
    }
}

