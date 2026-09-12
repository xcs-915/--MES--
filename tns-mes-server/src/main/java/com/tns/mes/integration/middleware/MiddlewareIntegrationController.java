package com.tns.mes.integration.middleware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.integration.ExternalApiClient;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * SAP 集成中台接口（推送类）手动调用端点。
 * 供后期业务模块调用联调，也可通过接口管理页面手工触发测试。
 */
@RestController
@RequestMapping("/api/v1/integrations/middleware")
public class MiddlewareIntegrationController {
    private final MiddlewareIntegrationService service;
    private final MiddlewareApiClient client;
    private final MiddlewareTokenManager tokenManager;
    private final MiddlewareProperties properties;
    private final ObjectMapper mapper;

    public MiddlewareIntegrationController(MiddlewareIntegrationService service,
                                           MiddlewareApiClient client,
                                           MiddlewareTokenManager tokenManager,
                                           MiddlewareProperties properties,
                                           ObjectMapper mapper) {
        this.service = service;
        this.client = client;
        this.tokenManager = tokenManager;
        this.properties = properties;
        this.mapper = mapper;
    }

    /** 领料过账（发货指令）。老MES: MES_PostPickingList_ViewList → /warehouse/deliveryCommandHeader/add */
    @PostMapping("/picking-lists/post")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> postPickingList(@RequestBody JsonNode payload, HttpServletRequest request) {
        requirePayload(payload);
        return ApiResponse.ok(service.postPickingList(payload), id(request));
    }

    /** 工单上料确认（审核+保存发料）。老MES: MES_PostMOFeeding_ViewList → /warehouse/deliveryHeader/approvalAndSave */
    @PostMapping("/mo-feeding/post")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> postMoFeeding(@RequestBody JsonNode payload, HttpServletRequest request) {
        requirePayload(payload);
        return ApiResponse.ok(service.postMoFeeding(payload), id(request));
    }

    /** 报工推送（物料消耗）。老MES: MES_PostFeedback_ViewList → /consumeMaterialOrderService/.../saveConsumeMaterialData */
    @PostMapping("/feedback/post")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> postFeedback(@RequestBody JsonNode payload, HttpServletRequest request) {
        requirePayload(payload);
        return ApiResponse.ok(service.postFeedback(payload), id(request));
    }

    /** 完工入库推送。老MES: MES_PostFinishEntity_ViewList → /warehouse/receiveCommand/add */
    @PostMapping("/finish-entity/post")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> postFinishEntity(@RequestBody JsonNode payload, HttpServletRequest request) {
        requirePayload(payload);
        return ApiResponse.ok(service.postFinishEntity(payload), id(request));
    }

    /** 其他入库推送。老MES: MES_PostFinishEntityOter_ViewLsit → /warehouse/receiveCommand/add */
    @PostMapping("/finish-entity-other/post")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> postFinishEntityOther(@RequestBody JsonNode payload, HttpServletRequest request) {
        requirePayload(payload);
        return ApiResponse.ok(service.postFinishEntityOther(payload), id(request));
    }

    /** 入库指令关闭。老MES: MES_receiveCommand_ViewList → PUT /warehouse/receiveCommand/close */
    @PostMapping("/receive-command/close")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> closeReceiveCommand(@RequestBody JsonNode payload, HttpServletRequest request) {
        requirePayload(payload);
        return ApiResponse.ok(service.closeReceiveCommand(payload), id(request));
    }

    /** 线边仓退料推送。老MES: MES_PostProductBack_ViewList → /warehouse/receiveCommand/add */
    @PostMapping("/product-back/post")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> postProductBack(@RequestBody JsonNode payload, HttpServletRequest request) {
        requirePayload(payload);
        return ApiResponse.ok(service.postProductBack(payload), id(request));
    }

    /** 退料到仓库（加工费退料单）。老MES: MES_PostProductBack_WaerHost_ViewList → /returnMaterialOrderService/.../saveReturnMaterialData */
    @PostMapping("/return-material/post")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> postReturnMaterial(@RequestBody JsonNode payload, HttpServletRequest request) {
        requirePayload(payload);
        return ApiResponse.ok(service.postReturnMaterial(payload), id(request));
    }

    /** 退料取消推送 SAP。老MES: MES_receive_prodCancel_ViewList → /warehouse/receiveHeader/prodCancelToSap */
    @PostMapping("/prod-cancel/post")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> postProdCancel(@RequestBody JsonNode payload, HttpServletRequest request) {
        requirePayload(payload);
        return ApiResponse.ok(service.postProdCancel(payload), id(request));
    }

    // --- 诊断 ---

    /** 查看中台连接配置与当前缓存 token（脱敏）。 */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ApiResponse<Map<String, Object>> status(HttpServletRequest request) {
        String token = tokenManager.peek();
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", properties.isEnabled());
        data.put("baseUrl", properties.getBaseUrl());
        data.put("username", properties.getUsername());
        data.put("tokenCached", token != null);
        data.put("tokenPreview", preview(token));
        return ApiResponse.ok(data, id(request));
    }

    /** 手动刷新中台 token。 */
    @PostMapping("/token/refresh")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<Map<String, Object>> refreshToken(HttpServletRequest request) {
        String token = tokenManager.refresh();
        Map<String, Object> data = new HashMap<>();
        data.put("refreshed", true);
        data.put("tokenPreview", preview(token));
        return ApiResponse.ok(data, id(request));
    }

    /** 通用中台调用（诊断/联调用）：method=GET|POST|PUT，path 为中台路径，body/query 原样透传。 */
    @PostMapping("/request")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<JsonNode> rawRequest(@RequestBody GenericRequest body, HttpServletRequest request) {
        if (body.getPath() == null || body.getPath().trim().isEmpty()) throw new BizException(4003, "error.validation");
        String method = body.getMethod() == null ? "POST" : body.getMethod().toUpperCase();
        ExternalApiClient.ExternalApiResponse response;
        if ("GET".equals(method)) {
            response = client.get(body.getPath().trim(), body.getQuery());
        } else if ("PUT".equals(method)) {
            response = client.put(body.getPath().trim(), body.getBody());
        } else {
            response = client.post(body.getPath().trim(), body.getBody());
        }
        JsonNode root;
        try {
            root = mapper.readTree(response.getBody() == null ? "{}" : response.getBody());
        } catch (Exception ex) {
            throw new BizException(5043, "middleware.error: invalid JSON response");
        }
        if (!MiddlewareTokenManager.isSuccessEnvelope(root)) {
            String message = root.hasNonNull("message") ? root.get("message").asText() : "http " + response.getStatus();
            throw new BizException(5042, "middleware.rejected: " + message);
        }
        return ApiResponse.ok(root, id(request));
    }

    private String preview(String token) {
        if (token == null) return null;
        return token.substring(0, Math.min(token.length(), 24)) + "...";
    }

    private void requirePayload(JsonNode payload) {
        if (payload == null || payload.isNull() || (payload.isObject() && payload.size() == 0)) {
            throw new BizException(4003, "error.validation");
        }
    }

    private String id(HttpServletRequest request) {
        Object value = request.getAttribute("requestId");
        return value == null ? null : value.toString();
    }

    public static class GenericRequest {
        private String path;
        private String method = "POST";
        private Map<String, Object> query;
        private JsonNode body;
        public String getPath() { return path; }
        public void setPath(String value) { path = value; }
        public String getMethod() { return method; }
        public void setMethod(String value) { method = value; }
        public Map<String, Object> getQuery() { return query; }
        public void setQuery(Map<String, Object> value) { query = value; }
        public JsonNode getBody() { return body; }
        public void setBody(JsonNode value) { body = value; }
    }
}
