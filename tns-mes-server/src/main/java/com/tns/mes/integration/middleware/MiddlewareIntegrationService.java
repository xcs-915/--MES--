package com.tns.mes.integration.middleware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.integration.ExternalApiClient;
import com.tns.mes.integration.sap.service.ApiCallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * SAP 集成中台业务接口（推送类，逻辑对齐老 MES data_sync_conf 中 121.41.173.27:9999 系列）。
 *
 * 覆盖四大业务（老 MES api_name → 本服务方法）：
 *   领料：MES_PostPickingList_ViewList   → postPickingList
 *   领料：MES_PostMOFeeding_ViewList     → postMoFeeding
 *   报工：MES_PostFeedback_ViewList      → postFeedback
 *   入库：MES_PostFinishEntity_ViewList  → postFinishEntity
 *   入库：MES_PostFinishEntityOter_ViewLsit → postFinishEntityOther
 *   入库：MES_receiveCommand_ViewList    → closeReceiveCommand
 *   退料：MES_PostProductBack_ViewList   → postProductBack
 *   退料：MES_PostProductBack_WaerHost_ViewList → postReturnMaterial
 *   退料：MES_receive_prodCancel_ViewList → postProdCancel
 *
 * 报文结构沿用老 MES 生产实测格式（见 docs/SAP_INTEGRATION_REPORT.md §六），
 * payload 由上层业务组装后传入；本层负责 Token、日志、统一响应校验。
 */
@Service
public class MiddlewareIntegrationService {
    private static final Logger log = LoggerFactory.getLogger(MiddlewareIntegrationService.class);
    private static final int MAX_LOG_BODY = 20000;

    // ---- 中台接口路径（与老 MES data_sync_conf 完全一致） ----
    public static final String PATH_POST_PICKING_LIST = "/warehouse/deliveryCommandHeader/add";
    public static final String PATH_POST_MO_FEEDING = "/warehouse/deliveryHeader/approvalAndSave";
    public static final String PATH_POST_FEEDBACK = "/consumeMaterialOrderService/consumeMaterialOrderHeader/saveConsumeMaterialData";
    public static final String PATH_POST_FINISH_ENTITY = "/warehouse/receiveCommand/add";
    public static final String PATH_CLOSE_RECEIVE_COMMAND = "/warehouse/receiveCommand/close";
    public static final String PATH_POST_PRODUCT_BACK = "/warehouse/receiveCommand/add";
    public static final String PATH_POST_RETURN_MATERIAL = "/returnMaterialOrderService/returnMaterialOrderHeader/saveReturnMaterialData";
    public static final String PATH_POST_PROD_CANCEL = "/warehouse/receiveHeader/prodCancelToSap";

    private final MiddlewareApiClient client;
    private final ApiCallLogService apiCallLogs;
    private final ObjectMapper mapper;

    public MiddlewareIntegrationService(MiddlewareApiClient client, ApiCallLogService apiCallLogs, ObjectMapper mapper) {
        this.client = client;
        this.apiCallLogs = apiCallLogs;
        this.mapper = mapper;
    }

    /** 领料过账（发货指令，驱动 SAP 发料出库）。老MES: MES_PostPickingList_ViewList。 */
    public JsonNode postPickingList(Object payload) {
        return execute("PICKING_POST", "领料过账", HttpMethod.POST, PATH_POST_PICKING_LIST, payload);
    }

    /** 工单上料确认（审核并保存发料，返回 SAP 报工凭证 confirmationGroup）。老MES: MES_PostMOFeeding_ViewList。 */
    public JsonNode postMoFeeding(Object payload) {
        return execute("MO_FEEDING_POST", "工单上料确认", HttpMethod.POST, PATH_POST_MO_FEEDING, payload);
    }

    /** 报工推送（物料消耗，含 SAP 预留号）。老MES: MES_PostFeedback_ViewList。 */
    public JsonNode postFeedback(Object payload) {
        return execute("FEEDBACK_POST", "报工推送", HttpMethod.POST, PATH_POST_FEEDBACK, payload);
    }

    /** 完工入库推送（移动类型 1101P 等）。老MES: MES_PostFinishEntity_ViewList。 */
    public JsonNode postFinishEntity(Object payload) {
        return execute("FINISH_ENTITY_POST", "完工入库", HttpMethod.POST, PATH_POST_FINISH_ENTITY, payload);
    }

    /** 其他入库推送（完工入库-其他）。老MES: MES_PostFinishEntityOter_ViewLsit。 */
    public JsonNode postFinishEntityOther(Object payload) {
        return execute("FINISH_ENTITY_OTHER_POST", "其他入库", HttpMethod.POST, PATH_POST_FINISH_ENTITY, payload);
    }

    /** 入库指令关闭。老MES: MES_receiveCommand_ViewList（PUT）。 */
    public JsonNode closeReceiveCommand(Object payload) {
        return execute("RECEIVE_COMMAND_CLOSE", "入库指令关闭", HttpMethod.PUT, PATH_CLOSE_RECEIVE_COMMAND, payload);
    }

    /** 线边仓退料推送（移动类型 1301XR 等）。老MES: MES_PostProductBack_ViewList。 */
    public JsonNode postProductBack(Object payload) {
        return execute("PRODUCT_BACK_POST", "线边仓退料", HttpMethod.POST, PATH_POST_PRODUCT_BACK, payload);
    }

    /** 退料到仓库（加工费退料单）。老MES: MES_PostProductBack_WaerHost_ViewList。 */
    public JsonNode postReturnMaterial(Object payload) {
        return execute("RETURN_MATERIAL_POST", "退料到仓库", HttpMethod.POST, PATH_POST_RETURN_MATERIAL, payload);
    }

    /** 退料取消推送 SAP。老MES: MES_receive_prodCancel_ViewList。 */
    public JsonNode postProdCancel(Object payload) {
        return execute("PROD_CANCEL_POST", "退料取消", HttpMethod.POST, PATH_POST_PROD_CANCEL, payload);
    }

    /** 统一执行：调用 → 记录 int_api_call_log → 校验响应信封（code==200 && success==true）。 */
    private JsonNode execute(String interfaceCode, String nameZh, HttpMethod method, String path, Object payload) {
        long start = System.currentTimeMillis();
        Integer status = null;
        String requestBody = toJson(payload);
        String responseBody = null;
        boolean success = false;
        String error = null;
        try {
            ExternalApiClient.ExternalApiResponse response = method == HttpMethod.GET
                    ? client.get(path, (Map<String, ?>) payload)
                    : (method == HttpMethod.PUT ? client.put(path, payload) : client.post(path, payload));
            status = response.getStatus();
            responseBody = response.getBody();
            if (!response.is2xx()) {
                throw new BizException(5041, "middleware.http-" + status + ": " + nameZh);
            }
            JsonNode root = mapper.readTree(responseBody == null ? "{}" : responseBody);
            if (!MiddlewareTokenManager.isSuccessEnvelope(root)) {
                String message = root.hasNonNull("message") ? root.get("message").asText() : "unknown";
                throw new BizException(5042, "middleware.rejected[" + nameZh + "]: " + message);
            }
            success = true;
            log.info("[MIDDLEWARE] {} {} -> OK ({}ms)", nameZh, path, System.currentTimeMillis() - start);
            return root;
        } catch (BizException ex) {
            error = ex.getMessage();
            throw ex;
        } catch (Exception ex) {
            error = ex.getMessage();
            throw new BizException(5043, "middleware.error[" + nameZh + "]: " + ex.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - start;
            apiCallLogs.logCall(MiddlewareApiClient.SYSTEM_CODE, path, method.name(), null,
                    truncate(requestBody), status, truncate(responseBody), duration, success, error);
        }
    }

    private String toJson(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception ex) { return String.valueOf(value); }
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_LOG_BODY ? value : value.substring(0, MAX_LOG_BODY);
    }
}
