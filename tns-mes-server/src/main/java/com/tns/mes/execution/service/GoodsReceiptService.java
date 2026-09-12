package com.tns.mes.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.execution.domain.GoodsReceipt;
import com.tns.mes.execution.domain.QualityInspection;
import com.tns.mes.execution.domain.WorkReport;
import com.tns.mes.execution.repo.GoodsReceiptRepository;
import com.tns.mes.execution.repo.QualityInspectionRepository;
import com.tns.mes.execution.repo.WorkReportRepository;
import com.tns.mes.execution.web.ExecutionRequests.GoodsReceiptRequest;
import com.tns.mes.execution.web.ExecutionViews.GoodsReceiptView;
import com.tns.mes.integration.middleware.MiddlewareIntegrationService;
import com.tns.mes.integration.outbox.OutboxService;
import com.tns.mes.production.domain.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoodsReceiptService {
    private final GoodsReceiptRepository repository;
    private final WorkReportRepository workReports;
    private final QualityInspectionRepository inspections;
    private final ExecutionSupport support;
    private final MiddlewareIntegrationService middleware;
    private final OutboxService outbox;

    public GoodsReceiptService(GoodsReceiptRepository repository, WorkReportRepository workReports,
                               QualityInspectionRepository inspections, ExecutionSupport support,
                               MiddlewareIntegrationService middleware, OutboxService outbox) {
        this.repository = repository;
        this.workReports = workReports;
        this.inspections = inspections;
        this.support = support;
        this.middleware = middleware;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public PageResponse<GoodsReceiptView> page(String keyword, String status, int page, int size) {
        Specification<GoodsReceipt> spec = Specification.where(null);
        if (hasText(keyword)) {
            String term = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("receiptNo")), term),
                    cb.like(cb.lower(root.join("workOrder").get("orderNo")), term)));
        }
        if (hasText(status)) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase()));
        Page<GoodsReceipt> result = repository.findAll(spec, ExecutionSupport.page(page, size, "postingDate"));
        return ExecutionSupport.map(result, GoodsReceiptView::new);
    }

    @Transactional
    @Auditable(action = "CREATE", resource = "GOODS_RECEIPT")
    public GoodsReceiptView create(GoodsReceiptRequest request) {
        WorkOrder order = support.executableOrder(request.getWorkOrderId());
        QualityInspection inspection = inspections.findWithRelationsById(request.getInspectionId())
                .orElseThrow(() -> new BizException(4041, "error.not-found"));
        if (!order.getId().equals(inspection.getWorkOrder().getId()) || !"COMPLETION".equals(inspection.getInspectionType()) || !"PASSED".equals(inspection.getStatus())) {
            throw new BizException(4092, "execution.completion-inspection-required");
        }
        WorkReport report = null;
        if (request.getWorkReportId() != null) {
            report = workReports.findWithRelationsById(request.getWorkReportId()).orElseThrow(() -> new BizException(4041, "error.not-found"));
            if (!order.getId().equals(report.getWorkOrder().getId()) || !"POSTED".equals(report.getStatus())) throw new BizException(4003, "error.validation");
        }
        if (request.getQuantity().compareTo(inspection.getQualifiedQuantity()) > 0) throw new BizException(4003, "execution.quantity-exceeds-inspection");
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptNo(ExecutionSupport.documentNo("GR"));
        receipt.setWorkOrder(order);
        receipt.setWorkReport(report);
        receipt.setInspection(inspection);
        receipt.setQuantity(request.getQuantity());
        receipt.setUnit(request.getUnit().trim());
        receipt.setBatchNo(trim(request.getBatchNo()));
        receipt.setWarehouseCode(request.getWarehouseCode().trim());
        receipt.setStorageLocation(request.getStorageLocation().trim());
        receipt.setMovementType(hasText(request.getMovementType()) ? request.getMovementType().trim() : "1101P");
        receipt.setOperatorCode(request.getOperatorCode().trim());
        receipt.setPostingDate(request.getPostingDate() == null ? Instant.now() : request.getPostingDate());
        receipt.setRemark(trim(request.getRemark()));
        return new GoodsReceiptView(repository.save(receipt));
    }

    @Transactional(noRollbackFor = BizException.class)
    @Auditable(action = "POST", resource = "GOODS_RECEIPT")
    public GoodsReceiptView post(Long id) {
        GoodsReceipt receipt = repository.findLockedById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
        if (!"DRAFT".equals(receipt.getStatus()) && !"FAILED".equals(receipt.getStatus())) throw new BizException(4092, "error.invalid-state");
        if (!"PASSED".equals(receipt.getInspection().getStatus())) throw new BizException(4092, "execution.completion-inspection-required");
        WorkOrder lockedOrder = support.lockOrder(receipt.getWorkOrder().getId());
        receipt.setWorkOrder(lockedOrder);
        BigDecimal posted = repository.sumPostedByWorkOrderId(lockedOrder.getId());
        if (zero(posted).add(receipt.getQuantity()).compareTo(receipt.getWorkOrder().getCompletedQuantity()) > 0) {
            throw new BizException(4003, "execution.quantity-exceeds-reported");
        }
        BigDecimal inspected = repository.sumPostedByInspectionId(receipt.getInspection().getId());
        if (zero(inspected).add(receipt.getQuantity()).compareTo(receipt.getInspection().getQualifiedQuantity()) > 0) {
            throw new BizException(4003, "execution.quantity-exceeds-inspection");
        }
        if (receipt.getWorkReport() != null) {
            BigDecimal reportPosted = repository.sumPostedByWorkReportId(receipt.getWorkReport().getId());
            if (zero(reportPosted).add(receipt.getQuantity()).compareTo(receipt.getWorkReport().getQualifiedQuantity()) > 0) {
                throw new BizException(4003, "execution.quantity-exceeds-reported");
            }
        }
        receipt.setStatus("POSTING");
        repository.saveAndFlush(receipt);
        try {
            JsonNode response = middleware.postFinishEntity(payload(receipt));
            receipt.setStatus("POSTED");
            receipt.setExternalDocId(ExecutionSupport.resultText(response, "docId", "id"));
            receipt.setExternalMessage(response.path("message").asText(null));
            receipt.setPostedAt(Instant.now());
            if (receipt.getWorkOrder().getCompletedQuantity().compareTo(receipt.getWorkOrder().getQuantity()) >= 0) receipt.getWorkOrder().setStatus("COMPLETED");
            outbox.enqueue("GOODS_RECEIPT", String.valueOf(receipt.getId()), "GOODS_RECEIPT_POSTED", event(receipt));
            return new GoodsReceiptView(repository.save(receipt));
        } catch (BizException ex) {
            receipt.setStatus("FAILED");
            receipt.setExternalMessage(truncate(ex.getMessage()));
            repository.saveAndFlush(receipt);
            throw ex;
        }
    }

    private Map<String, Object> payload(GoodsReceipt receipt) {
        WorkOrder order = receipt.getWorkOrder();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("baseLineNo", "");
        item.put("baseEntry", receipt.getReceiptNo());
        item.put("lineNo", 1);
        item.put("productId", order.getProduct().getCode());
        item.put("productName", order.getProduct().getNameZh());
        item.put("unit", receipt.getUnit());
        item.put("productStoreId", receipt.getWarehouseCode());
        item.put("facilityId", receipt.getStorageLocation());
        item.put("quantity", receipt.getQuantity());
        item.put("productDetailId", receipt.getBatchNo());
        item.put("receiveCommandItemDetailList", new ArrayList<>());
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("docStatus", "2");
        body.put("businessStatus", "1");
        body.put("movementTypeId", receipt.getMovementType());
        body.put("productStoreId", receipt.getWarehouseCode());
        body.put("orderNo", order.getOrderNo());
        body.put("baseEntry", receipt.getReceiptNo());
        body.put("facilityId", receipt.getStorageLocation());
        body.put("postingDate", ExecutionSupport.externalTime(receipt.getPostingDate()));
        body.put("sender", order.getOrderNo() + ";" + order.getProduct().getNameZh() + ";" + nullSafe(order.getProductionSupervisor()));
        body.put("receiveCommandItemList", items);
        return body;
    }

    private Map<String, Object> event(GoodsReceipt receipt) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("receiptNo", receipt.getReceiptNo()); value.put("orderNo", receipt.getWorkOrder().getOrderNo());
        value.put("quantity", receipt.getQuantity()); value.put("status", receipt.getStatus()); value.put("externalDocId", receipt.getExternalDocId());
        return value;
    }

    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private static String trim(String value) { return hasText(value) ? value.trim() : null; }
    private static String nullSafe(String value) { return value == null ? "" : value; }
    private static String truncate(String value) { return value == null || value.length() <= 1000 ? value : value.substring(0, 1000); }
}
