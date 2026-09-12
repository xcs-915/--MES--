package com.tns.mes.integration.middleware;

import com.tns.mes.common.exception.BizException;
import com.tns.mes.integration.ExternalApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 中台 HTTP 客户端：自动附加 x-access-token，401 时刷新 token 重试一次。
 */
@Component
public class MiddlewareApiClient {
    private static final Logger log = LoggerFactory.getLogger(MiddlewareApiClient.class);
    public static final String SYSTEM_CODE = "MIDDLEWARE";

    private final ExternalApiClient client;
    private final MiddlewareProperties properties;
    private final MiddlewareTokenManager tokenManager;

    public MiddlewareApiClient(ExternalApiClient client, MiddlewareProperties properties,
                               MiddlewareTokenManager tokenManager) {
        this.client = client;
        this.properties = properties;
        this.tokenManager = tokenManager;
    }

    /** 发送 GET 请求（带 token，401 自动重试一次）。 */
    public ExternalApiClient.ExternalApiResponse get(String path, Map<String, ?> query) {
        return executeWithRetry(HttpMethod.GET, path, query, null);
    }

    /** 发送 POST 请求（带 token，401 自动重试一次）。 */
    public ExternalApiClient.ExternalApiResponse post(String path, Object body) {
        return executeWithRetry(HttpMethod.POST, path, null, body);
    }

    /** 发送 PUT 请求（带 token，401 自动重试一次）。 */
    public ExternalApiClient.ExternalApiResponse put(String path, Object body) {
        return executeWithRetry(HttpMethod.PUT, path, null, body);
    }

    private ExternalApiClient.ExternalApiResponse executeWithRetry(HttpMethod method, String path,
                                                                   Map<String, ?> query, Object body) {
        requireEnabled();
        ExternalApiClient.ExternalApiResponse response = doExecute(method, path, query, body);
        if (response.getStatus() == 401) {
            log.warn("[MIDDLEWARE] 401 received, refreshing token and retrying once: {} {}", method, path);
            tokenManager.invalidate();
            response = doExecute(method, path, query, body);
        }
        return response;
    }

    private ExternalApiClient.ExternalApiResponse doExecute(HttpMethod method, String path,
                                                            Map<String, ?> query, Object body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-access-token", tokenManager.getToken());
        headers.put("Accept", "application/json");
        return client.execute(properties.getBaseUrl(), path, method, headers, query, body);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) throw new BizException(5031, "middleware.disabled");
    }
}
