package com.tns.mes.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/** Common bounded HTTP client used by external-system adapters. */
public class ExternalApiClient {
    private static final Logger log = LoggerFactory.getLogger(ExternalApiClient.class);
    private final RestTemplate restTemplate;

    public ExternalApiClient(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public ExternalApiResponse execute(String baseUrl, String path, HttpMethod method,
                                       Map<String, String> headers, Map<String, ?> query, Object body) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("External base URL is not configured");
        String normalizedPath = path == null ? "" : path.trim();
        if (!normalizedPath.isEmpty() && !normalizedPath.startsWith("/")) normalizedPath = "/" + normalizedPath;
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl.replaceAll("/$", "") + normalizedPath)
                .queryParams(toQueryParams(query)).build().encode().toUri();
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) headers.forEach(httpHeaders::set);
        log.info("[ExternalAPI] {} {}", method == null ? "GET" : method, uri);
        if (query != null && !query.isEmpty()) log.info("[ExternalAPI] Query params: {}", query);
        if (headers != null) {
            HttpHeaders masked = new HttpHeaders();
            headers.forEach((k, v) -> {
                if ("Authorization".equalsIgnoreCase(k) && v != null) masked.set(k, v.substring(0, Math.min(v.length(), 15)) + "***MASKED***");
                else masked.set(k, v);
            });
            log.info("[ExternalAPI] Headers: {}", masked);
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, method == null ? HttpMethod.GET : method,
                    new HttpEntity<>(body, httpHeaders), String.class);
            String ct = response.getHeaders().getContentType() == null ? "unknown" : response.getHeaders().getContentType().toString();
            String preview = response.getBody() == null ? "<null>" : response.getBody().substring(0, Math.min(response.getBody().length(), 200));
            log.info("[ExternalAPI] Response: {} {} | body preview: {}", response.getStatusCodeValue(), ct, preview);
            return new ExternalApiResponse(response.getStatusCodeValue(), response.getHeaders(), response.getBody());
        } catch (RestClientException ex) {
            log.error("[ExternalAPI] Request failed: {}", ex.getMessage());
            throw new ExternalApiException("External request failed: " + ex.getMessage(), ex);
        }
    }

    private org.springframework.util.MultiValueMap<String, String> toQueryParams(Map<String, ?> query) {
        org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        if (query != null) query.forEach((key, value) -> { if (value != null) params.add(key, String.valueOf(value)); });
        return params;
    }

    public static class ExternalApiResponse {
        private final int status;
        private final HttpHeaders headers;
        private final String body;
        public ExternalApiResponse(int status, HttpHeaders headers, String body) { this.status = status; this.headers = headers; this.body = body; }
        public int getStatus() { return status; }
        public HttpHeaders getHeaders() { return headers == null ? new HttpHeaders() : headers; }
        public String getBody() { return body; }
        public boolean is2xx() { return status >= 200 && status < 300; }
    }

    public static class ExternalApiException extends RuntimeException {
        public ExternalApiException(String message, Throwable cause) { super(message, cause); }
    }
}
