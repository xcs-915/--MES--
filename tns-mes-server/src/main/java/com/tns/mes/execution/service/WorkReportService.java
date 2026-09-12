package com.tns.mes.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.execution.domain.WorkReport;
import com.tns.mes.execution.repo.QualityInspectionRepository;
import com.tns.mes.execution.repo.WorkReportRepository;
import com.tns.mes.execution.web.ExecutionRequests.WorkReportRequest;
import com.tns.mes.execution.web.ExecutionViews.WorkReportView;
import com.tns.mes.integration.middleware.MiddlewareIntegrationService;
import com.tns.mes.integration.outbox.OutboxService;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.domain.WorkOrderOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WorkReportService {
    private final WorkReportRepository repository;
    private final QualityInspectionRepository inspections;
    private final ExecutionSupport support;
    private final MiddlewareIntegrationService middleware;
    private final OutboxService outbox;

    public WorkReportService(WorkReportRepository repository, QualityInspectionRepository inspections,
                             ExecutionSupport support, MiddlewareIntegrationService middleware, OutboxService outbox) {
        this.repository = repository;
        this.inspections = inspections;
        this.support = support;
        this.middleware = middleware;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkReportView> page(String keyword, String status, int page, int size) {
        Specification<WorkReport> spec = Specification.where(null);
        if (hasText(keyword)) {
            String term = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("reportNo")), term),
                    cb.like(cb.lower(root.join("workOrder").get("orderNo")), term)));
        }
        if (hasText(status)) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase()));
        Page<WorkReport> result = repository.findAll(spec, ExecutionSupport.page(page, size, "reportedAt"));
        return ExecutionSupport.map(result, WorkReportView::new);
    }

    @Transactional
    @Auditable(action = "CREATE", resource = "WORK_REPORT")
    public WorkReportView create(WorkReportRequest request) {
        WorkOrder order = support.executableOrder(request.getWorkOrderId());
        WorkOrderOperation operation = support.operation(order, request.getOperationId());
        WorkReport report = new WorkReport();
        report.setReportNo(ExecutionSupport.documentNo("WR"));
        report.setWorkOrder(order);
        report.setOperation(operation);
        report.setQualifiedQuantity(request.getQualifiedQuantity());
        report.setUnqualifiedQuantity(request.getUnqualifiedQuantity());
        report.setOperatorCode(request.getOperatorCode().trim());
        report.setShiftCode(trim(request.getShiftCode()));
        report.setEquipmentCode(trim(request.getEquipmentCode()));
        report.setReportedAt(request.getReportedAt() == null ? Instant.now() : request.getReportedAt());
        report.setRemark(trim(request.getRemark()));
        return new WorkReportView(repository.save(report));
    }

    @Transactional(noRollbackFor = BizException.class)
    @Auditable(action = "POST", resource = "WORK_REPORT")
    public WorkReportView post(Long id) {
        WorkReport report = repository.findLockedById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
        if (!"DRAFT".equals(report.getStatus()) && !"FAILED".equals(report.getStatus())) throw new BizException(4092, "error.invalid-state");
        Long orderId = report.getWorkOrder().getId();
        boolean firstRequested = inspections.existsByWorkOrder_IdAndInspectionType(orderId, "FIRST");
        boolean firstPassed = inspections.existsByWorkOrder_IdAndInspectionTypeAndStatus(orderId, "FIRST", "PASSED");
        boolean firstPending = inspections.existsByWorkOrder_IdAndInspectionTypeAndStatusIn(orderId, "FIRST",
                Arrays.asList("PENDING", "IN_PROGRESS"));
        if (firstRequested && (!firstPassed || firstPending)) {
            throw new BizException(4092, "execution.first-inspection-required");
        }
        WorkOrder lockedOrder = support.lockOrder(report.getWorkOrder().getId());
        report.setWorkOrder(lockedOrder);
        ensureCapacity(report);
        report.setStatus("POSTING");
        repository.saveAndFlush(report);
        try {
            JsonNode response = middleware.postMoFeeding(payload(report));
            report.setStatus("POSTED");
            report.setConfirmationGroup(ExecutionSupport.resultText(response, "confirmationGroup", "operationConfirmation"));
            report.setExternalDocId(ExecutionSupport.resultText(response, "id", "docId", "workCheckOrderld"));
            report.setExternalMessage(response.path("message").asText(null));
            report.setPostedAt(Instant.now());
            applyQuantity(report);
            outbox.enqueue("WORK_REPORT", String.valueOf(report.getId()), "WORK_REPORT_POSTED", event(report));
            return new WorkReportView(repository.save(report));
        } catch (BizException ex) {
            report.setStatus("FAILED");
            report.setExternalMessage(truncate(ex.getMessage()));
            repository.saveAndFlush(report);
            throw ex;
        }
    }

    private void ensureCapacity(WorkReport report) {
        WorkOrder order = report.getWorkOrder();
        BigDecimal remaining = order.getQuantity().subtract(order.getCompletedQuantity());
        if (report.getQualifiedQuantity().compareTo(remaining) > 0) throw new BizException(4003, "execution.quantity-exceeds-order");
        if (report.getOperation() != null) {
            BigDecimal completed = zero(report.getOperation().getCompletedQuantity());
            BigDecimal planned = zero(report.getOperation().getPlannedQuantity());
            if (completed.add(report.getQualifiedQuantity()).compareTo(planned) > 0) throw new BizException(4003, "execution.quantity-exceeds-operation");
        }
    }

    private void applyQuantity(WorkReport report) {
        WorkOrder order = report.getWorkOrder();
        order.setCompletedQuantity(zero(order.getCompletedQuantity()).add(report.getQualifiedQuantity()));
        order.setConfirmedYieldQuantity(zero(order.getConfirmedYieldQuantity()).add(report.getQualifiedQuantity()));
        if ("RELEASED".equals(order.getStatus())) order.setStatus("IN_PROGRESS");
        if (report.getOperation() != null) {
            WorkOrderOperation operation = report.getOperation();
            operation.setCompletedQuantity(zero(operation.getCompletedQuantity()).add(report.getQualifiedQuantity()));
            operation.setConfirmedYieldQuantity(zero(operation.getConfirmedYieldQuantity()).add(report.getQualifiedQuantity()));
            operation.setStatus(operation.getCompletedQuantity().compareTo(operation.getPlannedQuantity()) >= 0 ? "COMPLETED" : "IN_PROGRESS");
        }
    }

    private Map<String, Object> payload(WorkReport report) {
        WorkOrder order = report.getWorkOrder();
        WorkOrderOperation operation = report.getOperation();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("productionOrderId", order.getOrderNo());
        body.put("productId", order.getProduct().getCode());
        body.put("productName", order.getProduct().getNameZh());
        body.put("processId", operation == null ? null : first(operation.getOperationCode(), String.valueOf(operation.getSequenceNo())));
        body.put("processName", operation == null ? null : operation.getOperationName());
        body.put("qualifiedQuantity", report.getQualifiedQuantity());
        body.put("unqualifiedQuantity", report.getUnqualifiedQuantity());
        body.put("defectiveProductQuantity", report.getUnqualifiedQuantity());
        body.put("productStoreId", first(order.getProductionPlant(), order.getPlant()));
        body.put("unit", first(order.getProductionUnit(), order.getProduct().getUnit()));
        body.put("workCheckOrderTime", ExecutionSupport.externalTime(report.getReportedAt()));
        body.put("statisticsHour", 0);
        body.put("statisticalMachine", 0);
        body.put("staffHour", 0);
        body.put("equipmentHour", 0);
        body.put("materialCost", 0);
        body.put("mouldCode", "");
        body.put("mouldQuantity", "");
        body.put("orderType", order.getOrderType());
        body.put("personnel", report.getOperatorCode());
        body.put("equipment", report.getEquipmentCode());
        body.put("production_feedback_id", report.getId());
        body.put("operationConfirmation", report.getConfirmationGroup());
        body.put("baseEntry", report.getReportNo());
        return body;
    }

    private Map<String, Object> event(WorkReport report) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("reportNo", report.getReportNo()); value.put("orderNo", report.getWorkOrder().getOrderNo());
        value.put("qualifiedQuantity", report.getQualifiedQuantity()); value.put("unqualifiedQuantity", report.getUnqualifiedQuantity());
        value.put("status", report.getStatus()); value.put("confirmationGroup", report.getConfirmationGroup());
        return value;
    }

    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private static String trim(String value) { return hasText(value) ? value.trim() : null; }
    private static String first(String left, String right) { return hasText(left) ? left : right; }
    private static String truncate(String value) { return value == null || value.length() <= 1000 ? value : value.substring(0, 1000); }
}
