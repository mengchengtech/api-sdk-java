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

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestOption {
    private final SignedBy signedBy;
    private final Integer timeout;
    private final Map<String, String> query;
    private final Map<String, String> headers;
    private final String contentType;
    private final HttpEntity entity;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private SignedBy signedBy;
        private Integer timeout;
        private final HashMap<String, String> query;
        private final HashMap<String, String> headers;
        private String contentType;
        private HttpEntity entity;

        private Builder() {
            this.query = new HashMap<>();
            this.headers = new HashMap<>();
        }

        public Builder signedBy(SignedBy signedBy) {
            this.signedBy = signedBy;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder addQuery(String name, Object value) {
            this.query.put(name, value.toString());
            return this;
        }

        public Builder addQueries(Map<String, Object> query) {
            if (query != null && !query.isEmpty()) {
                query.forEach((k, v) -> this.query.put(k, v.toString()));
            }
            return this;
        }

        public Builder addHeader(String name, Object value) {
            this.headers.put(name, value.toString());
            return this;
        }

        public Builder addHeaders(Map<String, Object> headers) {
            if (headers != null && !headers.isEmpty()) {
                headers.forEach((k, v) -> this.headers.put(k, v.toString()));
            }
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
            return new RequestOption(signedBy, timeout, query, headers, contentType, entity);
        }
    }
}
