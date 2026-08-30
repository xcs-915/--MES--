package com.tns.mes.integration.sap;

import com.fasterxml.jackson.databind.JsonNode;
import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.integration.ExternalApiClient;
import com.tns.mes.integration.sap.domain.ApiCallLog;
import com.tns.mes.integration.sap.service.ApiCallLogService;
import com.tns.mes.integration.sync.SyncJobService;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations")
public class SapIntegrationController {
    private final SapSyncService service;
    private final SyncJobService syncJobs;
    private final ApiCallLogService apiCallLogs;
    public SapIntegrationController(SapSyncService service, SyncJobService syncJobs, ApiCallLogService apiCallLogs) {
        this.service = service;
        this.syncJobs = syncJobs;
        this.apiCallLogs = apiCallLogs;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ApiResponse<PageResponse<ApiCallLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) String system,
            HttpServletRequest request) {
        return ApiResponse.ok(apiCallLogs.findByFilters(system, endpoint, page, size), id(request));
    }

    // --- SAP endpoints ---

    @PostMapping("/sap/request")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<ExternalApiClient.ExternalApiResponse> request(@Valid @RequestBody GenericRequest body, HttpServletRequest request) {
        if (body.path.contains("://") || body.path.startsWith("//")) throw new BizException(4003, "error.validation");
        HttpMethod method;
        try { method = HttpMethod.valueOf(body.method == null ? "GET" : body.method.toUpperCase()); }
        catch (Exception ex) { throw new BizException(4003, "error.validation"); }
        return ApiResponse.ok(service.request(method, body.path, body.query, body.body), id(request));
    }

    @PostMapping("/sap/products/sync")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<SapSyncService.SyncResult> syncProducts(@RequestBody(required = false) SyncRequest body, HttpServletRequest request) {
        SyncRequest value = body == null ? new SyncRequest() : body;
        return ApiResponse.ok(syncJobs.runByCode(SyncJobService.PRODUCT_JOB, "MANUAL", value.path, value.effectiveQuery("LastChangeDateTime")), id(request));
    }

    @PostMapping("/sap/products/{code}/sync")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<SapSyncService.SyncResult> syncProduct(@PathVariable String code, HttpServletRequest request) {
        return ApiResponse.ok(syncJobs.runByCode(SyncJobService.PRODUCT_JOB, "MANUAL", null,
                Collections.<String, Object>singletonMap("$filter", "Product eq '" + code.trim().replace("'", "''") + "'")), id(request));
    }

    @PostMapping("/sap/work-orders/sync")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<SapSyncService.SyncResult> syncWorkOrders(@RequestBody(required = false) SyncRequest body, HttpServletRequest request) {
        SyncRequest value = body == null ? new SyncRequest() : body;
        return ApiResponse.ok(syncJobs.runByCode(SyncJobService.WORK_ORDER_JOB, "MANUAL", value.path, value.effectiveQuery("LastChangeDateTime")), id(request));
    }

    @PostMapping("/sap/work-orders/{orderNo}/sync")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<SapSyncService.SyncResult> syncWorkOrder(@PathVariable String orderNo, HttpServletRequest request) {
        return ApiResponse.ok(syncJobs.runByCode(SyncJobService.WORK_ORDER_JOB, "MANUAL", null,
                Collections.<String, Object>singletonMap("$filter", "ManufacturingOrder eq '" + orderNo.trim().replace("'", "''") + "'")), id(request));
    }

    @PostMapping("/sap/batches/sync")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<SapSyncService.SyncResult> syncBatches(@RequestBody(required = false) SyncRequest body, HttpServletRequest request) {
        SyncRequest value = body == null ? new SyncRequest() : body;
        return ApiResponse.ok(syncJobs.runByCode(SyncJobService.BATCH_JOB, "MANUAL", value.path, value.effectiveQuery("LastChangeDateTime")), id(request));
    }

    @PostMapping("/sap/batches/{batchNo}/sync")
    @PreAuthorize("hasAuthority('INTEGRATION_WRITE')")
    public ApiResponse<SapSyncService.SyncResult> syncBatch(@PathVariable String batchNo, HttpServletRequest request) {
        return ApiResponse.ok(syncJobs.runByCode(SyncJobService.BATCH_JOB, "MANUAL", null,
                Collections.<String, Object>singletonMap("$filter", "Batch eq '" + batchNo.trim().replace("'", "''") + "'")), id(request));
    }

    private String id(HttpServletRequest request) { Object value=request.getAttribute("requestId"); return value == null ? null : value.toString(); }
    public static class SyncRequest {
        private String path;
        private Map<String, Object> query = Collections.emptyMap();
        private Integer minutes = 15;
        public String getPath(){return path;} public void setPath(String v){path=v;}
        public Map<String,Object> getQuery(){return query;} public void setQuery(Map<String,Object> v){query=v==null?Collections.emptyMap():v;}
        public Integer getMinutes(){return minutes;} public void setMinutes(Integer v){minutes=v;}
        Map<String,Object> effectiveQuery(String changedField) {
            Map<String,Object> result = new java.util.HashMap<>(query == null ? Collections.emptyMap() : query);
            if (!result.containsKey("$filter")) {
                int window = minutes == null ? 15 : Math.max(1, Math.min(minutes, 1440));
                // SAP time is UTC; subtract 8 hours to align with China timezone (UTC+8)
                String since = OffsetDateTime.now(ZoneOffset.UTC).minus(window, ChronoUnit.MINUTES).minusHours(8)
                        .truncatedTo(ChronoUnit.SECONDS).toString();
                result.put("$filter", changedField + " ge datetimeoffset'" + since + "'");
            }
            return result;
        }
    }
    public static class GenericRequest { @NotBlank private String path; private String method = "GET"; private Map<String,Object> query = Collections.emptyMap(); private JsonNode body; public String getPath(){return path;} public void setPath(String v){path=v;} public String getMethod(){return method;} public void setMethod(String v){method=v;} public Map<String,Object> getQuery(){return query;} public void setQuery(Map<String,Object> v){query=v==null?Collections.emptyMap():v;} public JsonNode getBody(){return body;} public void setBody(JsonNode v){body=v;} }
}
