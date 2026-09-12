package com.tns.mes.integration.middleware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.integration.ExternalApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 中台 Token 管理器。
 * 逻辑对齐老 MES（ERP_SAP_GetToken_DoMethod）：
 *   1. 缓存有效期内的 token 直接复用（老 MES 写入 dbo.token 表缓存 3 小时）；
 *   2. 过期后调用 POST /sys/loginGetToken 重新获取（账号 MES-JX）；
 *   3. 遇到 401 由调用方 invalidate 后重试。
 */
@Component
public class MiddlewareTokenManager {
    private static final Logger log = LoggerFactory.getLogger(MiddlewareTokenManager.class);
    static final String TOKEN_PATH = "/sys/loginGetToken";

    private final ExternalApiClient client;
    private final MiddlewareProperties properties;
    private final ObjectMapper mapper;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public MiddlewareTokenManager(ExternalApiClient client, MiddlewareProperties properties, ObjectMapper mapper) {
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
    }

    /** 获取有效 token（过期自动刷新，线程安全）。 */
    public synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }
        return refresh();
    }

    /** 强制重新获取 token。 */
    public synchronized String refresh() {
        requireEnabled();
        Map<String, Object> body = new HashMap<>();
        body.put("username", properties.getUsername());
        body.put("password", properties.getPassword() == null ? "" : properties.getPassword());
        long start = System.currentTimeMillis();
        Integer status = null;
        String responseBody = null;
        boolean success = false;
        String error = null;
        try {
            ExternalApiClient.ExternalApiResponse response = client.execute(
                    properties.getBaseUrl(), TOKEN_PATH, HttpMethod.POST, null, null, body);
            status = response.getStatus();
            responseBody = response.getBody();
            if (!response.is2xx()) {
                throw new BizException(5032, "middleware.token.http-" + status);
            }
            JsonNode root = mapper.readTree(responseBody == null ? "{}" : responseBody);
            if (!isSuccessEnvelope(root)) {
                String message = root.hasNonNull("message") ? root.get("message").asText() : "unknown";
                throw new BizException(5033, "middleware.token.failed: " + message);
            }
            JsonNode result = root.get("result");
            String token = result != null && result.hasNonNull("token") ? result.get("token").asText() : null;
            if (token == null || token.trim().isEmpty()) {
                throw new BizException(5033, "middleware.token.empty");
            }
            cachedToken = token.trim();
            expiresAt = Instant.now().plusSeconds(properties.getTokenTtlMinutes() * 60L);
            success = true;
            log.info("[MIDDLEWARE] token refreshed (ttl={}min), username={}", properties.getTokenTtlMinutes(), properties.getUsername());
            return cachedToken;
        } catch (BizException ex) {
            error = ex.getMessage();
            throw ex;
        } catch (Exception ex) {
            error = ex.getMessage();
            throw new BizException(5033, "middleware.token.failed: " + ex.getMessage());
        } finally {
            log.info("[MIDDLEWARE] loginGetToken status={} success={} duration={}ms", status, success, System.currentTimeMillis() - start);
        }
    }

    /** 使缓存的 token 失效（收到 401 时调用）。 */
    public synchronized void invalidate() {
        cachedToken = null;
        expiresAt = Instant.EPOCH;
    }

    /** 查看当前缓存 token（可能为 null，仅供诊断）。 */
    public synchronized String peek() {
        return cachedToken;
    }

    /** 中台统一响应信封判定：code == 200 且 success == true。 */
    static boolean isSuccessEnvelope(JsonNode root) {
        return root != null
                && root.has("success") && root.get("success").asBoolean(false)
                && root.has("code") && root.get("code").asInt(-1) == 200;
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) throw new BizException(5031, "middleware.disabled");
    }
}
