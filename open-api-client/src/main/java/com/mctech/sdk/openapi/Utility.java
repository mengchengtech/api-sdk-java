package com.mctech.sdk.openapi;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class Utility {
    private static final String OpenApiPrefix = "x-iwop-";

    public static String buildCanonicalString(SignatureOption option) {
        List<String> itemsToSign = new ArrayList<>();
        itemsToSign.add(option.getMethod());
        itemsToSign.add(option.getContentType());
        //itemsToSign.add(option.ContentMd5);
        itemsToSign.add(option.getFormatedDate());

        Map<String, String> headers = option.getHeaders();
        List<String> keys = headers.keySet().stream()
                .filter(key -> key.toLowerCase().startsWith(OpenApiPrefix))
                .sorted()
                .collect(Collectors.toList());
        for (String key : keys) {
            itemsToSign.add(key + ":" + headers.get(key));
        }

        // add canonical resource
        String canonicalizedResource = buildCanonicalizedResource(option.getRequestUri());
        itemsToSign.add(canonicalizedResource);

        return StringUtils.join(itemsToSign, "\n");
    }

    @SneakyThrows
    private static String buildCanonicalizedResource(URI requestUri) {
        URIBuilder urlBuilder = new URIBuilder(requestUri);
        List<NameValuePair> params = urlBuilder.getQueryParams();
        if (!params.isEmpty()) {
            params.sort(Comparator.comparing(NameValuePair::getName));
            urlBuilder.removeQuery().addParameters(params);
        }
        return urlBuilder.toString();
    }
}
