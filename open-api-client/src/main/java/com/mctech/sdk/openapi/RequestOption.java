package com.mctech.sdk.openapi;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.HttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestOption {
    @Getter
    private final Integer timeout;
    @Getter
    private final Map<String, String> query;
    @Getter
    private final Map<String, String> headers;
    @Getter
    private final String contentType;
    @Getter
    private final HttpEntity entity;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Integer timeout;
        private final HashMap<String, String> query;
        private final HashMap<String, String> headers;
        private HttpEntity entity;
        private String contentType;

        private Builder() {
            this.query = new HashMap<>();
            this.headers = new HashMap<>();
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder query(String name, Object value) {
            this.query.put(name, value.toString());
            return this;
        }

        public Builder header(String name, Object value) {
            this.headers.put(name, value.toString());
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
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
            return new RequestOption(timeout, query, headers, contentType, entity);
        }
    }
}
