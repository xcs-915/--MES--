package com.tns.mes.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.execution.domain.MaterialIssue;
import com.tns.mes.execution.domain.MaterialIssueItem;
import com.tns.mes.execution.repo.MaterialIssueRepository;
import com.tns.mes.execution.web.ExecutionRequests.MaterialIssueItemRequest;
import com.tns.mes.execution.web.ExecutionRequests.MaterialIssueRequest;
import com.tns.mes.execution.web.ExecutionViews.MaterialIssueView;
import com.tns.mes.integration.middleware.MiddlewareIntegrationService;
import com.tns.mes.integration.outbox.OutboxService;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.domain.WorkOrderOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaterialIssueService {
    private final MaterialIssueRepository repository;
    private final ExecutionSupport support;
    private final MiddlewareIntegrationService middleware;
    private final OutboxService outbox;

    public MaterialIssueService(MaterialIssueRepository repository, ExecutionSupport support,
                                MiddlewareIntegrationService middleware, OutboxService outbox) {
        this.repository = repository;
        this.support = support;
        this.middleware = middleware;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public PageResponse<MaterialIssueView> page(String keyword, String status, int page, int size) {
        Specification<MaterialIssue> spec = Specification.where(null);
        if (hasText(keyword)) {
            String term = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("issueNo")), term),
                    cb.like(cb.lower(root.join("workOrder").get("orderNo")), term)));
        }
        if (hasText(status)) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase()));
        Page<MaterialIssue> result = repository.findAll(spec, ExecutionSupport.page(page, size, "postingDate"));
        return ExecutionSupport.map(result, MaterialIssueView::new);
    }

    @Transactional
    @Auditable(action = "CREATE", resource = "MATERIAL_ISSUE")
    public MaterialIssueView create(MaterialIssueRequest request) {
        WorkOrder order = support.executableOrder(request.getWorkOrderId());
        WorkOrderOperation operation = support.operation(order, request.getOperationId());
        MaterialIssue issue = new MaterialIssue();
        issue.setIssueNo(ExecutionSupport.documentNo("MI"));
        issue.setWorkOrder(order);
        issue.setOperation(operation);
        issue.setOperatorCode(request.getOperatorCode().trim());
        issue.setWarehouseCode(trim(request.getWarehouseCode()));
        issue.setPostingDate(request.getPostingDate() == null ? Instant.now() : request.getPostingDate());
        issue.setRemark(trim(request.getRemark()));
        int line = 1;
        for (MaterialIssueItemRequest source : request.getItems()) {
            MaterialIssueItem item = new MaterialIssueItem();
            item.setLineNo(line++);
            item.setProductCode(source.getProductCode().trim());
            item.setProductName(trim(source.getProductName()));
            item.setBatchNo(trim(source.getBatchNo()));
            item.setQuantity(source.getQuantity());
            item.setUnit(source.getUnit().trim());
            item.setStorageLocation(trim(source.getStorageLocation()));
            item.setReservationNo(trim(source.getReservationNo()));
            item.setReservationItem(trim(source.getReservationItem()));
            issue.addItem(item);
        }
        return new MaterialIssueView(repository.save(issue));
    }

    @Transactional(noRollbackFor = BizException.class)
    @Auditable(action = "POST", resource = "MATERIAL_ISSUE")
    public MaterialIssueView post(Long id) {
        MaterialIssue issue = repository.findLockedById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
        if (!"DRAFT".equals(issue.getStatus()) && !"FAILED".equals(issue.getStatus())) throw new BizException(4092, "error.invalid-state");
        issue.setStatus("POSTING");
        repository.saveAndFlush(issue);
        try {
            JsonNode response = middleware.postFeedback(payload(issue));
            issue.setStatus("POSTED");
            issue.setExternalDocId(ExecutionSupport.resultText(response, "docId", "id"));
            issue.setExternalMessage(response.path("message").asText(null));
            issue.setPostedAt(Instant.now());
            outbox.enqueue("MATERIAL_ISSUE", String.valueOf(issue.getId()), "MATERIAL_ISSUE_POSTED", event(issue));
            return new MaterialIssueView(repository.save(issue));
        } catch (BizException ex) {
            issue.setStatus("FAILED");
            issue.setExternalMessage(truncate(ex.getMessage()));
            repository.saveAndFlush(issue);
            throw ex;
        }
    }

    private Map<String, Object> payload(MaterialIssue issue) {
        WorkOrder order = issue.getWorkOrder();
        Map<String, Object> body = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MaterialIssueItem item : issue.getItems()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("no", String.format("%03d", item.getLineNo()));
            row.put("productId", item.getProductCode());
            row.put("productName", item.getProductName());
            row.put("currentPatchQuantity", item.getQuantity());
            row.put("unit", item.getUnit());
            row.put("facilityId", item.getStorageLocation());
            row.put("batchId", item.getBatchNo());
            row.put("reservation", item.getReservationNo());
            row.put("reservationItem", item.getReservationItem());
            rows.add(row);
        }
        body.put("consumeMaterialOrders", rows);
        body.put("productOrder", order.getOrderNo());
        body.put("orderType", order.getOrderType());
        body.put("plant", first(order.getProductionPlant(), order.getPlant()));
        body.put("postingDate", ExecutionSupport.externalTime(issue.getPostingDate()));
        body.put("isAccessoryBindingButton", "N");
        body.put("feeding_id", issue.getId());
        body.put("channel", "MES");
        body.put("baseEntry", issue.getIssueNo());
        return body;
    }

    private Map<String, Object> event(MaterialIssue issue) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("issueNo", issue.getIssueNo()); value.put("orderNo", issue.getWorkOrder().getOrderNo());
        value.put("status", issue.getStatus()); value.put("externalDocId", issue.getExternalDocId());
        return value;
    }

    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private static String trim(String value) { return hasText(value) ? value.trim() : null; }
    private static String first(String left, String right) { return hasText(left) ? left : right; }
    private static String truncate(String value) { return value == null || value.length() <= 1000 ? value : value.substring(0, 1000); }
}
