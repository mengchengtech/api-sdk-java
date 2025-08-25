package com.mctech.sdk.openapi;

import lombok.Getter;
import org.apache.http.HttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RequestOption {
    @Getter
    private final Map<String, String> query;
    @Getter
    private final Map<String, String> headers;
    @Getter
    private final HttpEntity entity;

    RequestOption(Map<String, String> query, Map<String, String> headers, HttpEntity entity) {
        this.query = query;
        this.headers = headers;
        this.entity = entity;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final HashMap<String, String> query;
        private final HashMap<String, String> headers;
        private HttpEntity entity;

        private Builder() {
            this.query = new HashMap<>();
            this.headers = new HashMap<>();
        }

        public Builder query(String name, Object value) {
            this.query.put(name, value.toString());
            return this;
        }

        public Builder header(String name, Object value) {
            this.headers.put(name, value.toString());
            return this;
        }

        public Builder body(String body) {
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            this.entity = new ByteArrayEntity(data);
            return this;
        }

        public Builder body(InputStream body) {
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(body);
            this.entity = entity;
            return this;
        }

        public RequestOption build() {
            return new RequestOption(this.query, this.headers, this.entity);
        }
    }
}
