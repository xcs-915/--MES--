package com.tns.mes.execution.service;

import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.execution.domain.QualityInspection;
import com.tns.mes.execution.domain.QualityInspectionItem;
import com.tns.mes.execution.domain.WorkReport;
import com.tns.mes.execution.repo.QualityInspectionRepository;
import com.tns.mes.execution.repo.WorkReportRepository;
import com.tns.mes.execution.web.ExecutionRequests.InspectionItemRequest;
import com.tns.mes.execution.web.ExecutionRequests.InspectionRequest;
import com.tns.mes.execution.web.ExecutionRequests.InspectionResultRequest;
import com.tns.mes.execution.web.ExecutionViews.InspectionView;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.domain.WorkOrderOperation;
import com.tns.mes.integration.outbox.OutboxService;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class InspectionService {
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("FIRST", "LAST", "PATROL", "COMPLETION", "GP"));
    private final QualityInspectionRepository repository;
    private final WorkReportRepository workReports;
    private final ExecutionSupport support;
    private final OutboxService outbox;

    public InspectionService(QualityInspectionRepository repository, WorkReportRepository workReports,
                             ExecutionSupport support, OutboxService outbox) {
        this.repository = repository;
        this.workReports = workReports;
        this.support = support;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public PageResponse<InspectionView> page(String keyword, String type, String status, int page, int size) {
        Specification<QualityInspection> spec = Specification.where(null);
        if (hasText(keyword)) {
            String term = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("inspectionNo")), term),
                    cb.like(cb.lower(root.join("workOrder").get("orderNo")), term)));
        }
        if (hasText(type)) spec = spec.and((root, query, cb) -> cb.equal(root.get("inspectionType"), normalizeType(type)));
        if (hasText(status)) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase(Locale.ROOT)));
        Page<QualityInspection> result = repository.findAll(spec, ExecutionSupport.page(page, size, "requestedAt"));
        return ExecutionSupport.map(result, InspectionView::new);
    }

    @Transactional
    @Auditable(action = "REQUEST", resource = "QUALITY_INSPECTION")
    public InspectionView request(InspectionRequest request) {
        WorkOrder order = support.executableOrder(request.getWorkOrderId());
        WorkOrderOperation operation = support.operation(order, request.getOperationId());
        WorkReport report = null;
        if (request.getWorkReportId() != null) {
            report = workReports.findWithRelationsById(request.getWorkReportId()).orElseThrow(() -> new BizException(4041, "error.not-found"));
            if (!order.getId().equals(report.getWorkOrder().getId())) throw new BizException(4003, "error.validation");
        }
        QualityInspection inspection = new QualityInspection();
        inspection.setInspectionNo(ExecutionSupport.documentNo("QI"));
        inspection.setInspectionType(normalizeType(request.getInspectionType()));
        inspection.setWorkOrder(order);
        inspection.setOperation(operation);
        inspection.setWorkReport(report);
        inspection.setSampleQuantity(request.getSampleQuantity());
        inspection.setRequestedBy(request.getRequestedBy().trim());
        inspection.setRequestedAt(request.getRequestedAt() == null ? Instant.now() : request.getRequestedAt());
        inspection.setRemark(trim(request.getRemark()));
        addItems(inspection, request.getItems(), false);
        return new InspectionView(repository.save(inspection));
    }

    @Transactional
    @Auditable(action = "INSPECT", resource = "QUALITY_INSPECTION")
    public InspectionView complete(Long id, InspectionResultRequest request) {
        QualityInspection inspection = repository.findWithRelationsById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
        if (!"PENDING".equals(inspection.getStatus()) && !"IN_PROGRESS".equals(inspection.getStatus())) throw new BizException(4092, "error.invalid-state");
        BigDecimal inspected = request.getQualifiedQuantity().add(request.getUnqualifiedQuantity());
        if (inspected.compareTo(inspection.getSampleQuantity()) != 0) throw new BizException(4003, "execution.inspection-quantity-mismatch");
        String result = request.getOverallResult().trim().toUpperCase(Locale.ROOT);
        if (!"PASS".equals(result) && !"FAIL".equals(result)) throw new BizException(4003, "error.validation");
        if ("FAIL".equals(result) && !hasText(request.getDisposition())) throw new BizException(4003, "execution.disposition-required");
        inspection.setQualifiedQuantity(request.getQualifiedQuantity());
        inspection.setUnqualifiedQuantity(request.getUnqualifiedQuantity());
        inspection.setInspectorCode(request.getInspectorCode().trim());
        inspection.setInspectedAt(Instant.now());
        inspection.setStatus("PASS".equals(result) ? "PASSED" : "FAILED");
        inspection.setDefectCode(trim(request.getDefectCode()));
        inspection.setDisposition(trim(request.getDisposition()));
        inspection.setRemark(trim(request.getRemark()));
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            inspection.getItems().clear();
            addItems(inspection, request.getItems(), true);
        } else {
            for (QualityInspectionItem item : inspection.getItems()) item.setResult(result);
        }
        if ("PASS".equals(result) && inspection.getItems().stream().anyMatch(item -> "FAIL".equals(item.getResult()))) {
            throw new BizException(4003, "execution.item-result-conflict");
        }
        java.util.Map<String, Object> event = new java.util.LinkedHashMap<>();
        event.put("inspectionNo", inspection.getInspectionNo()); event.put("inspectionType", inspection.getInspectionType());
        event.put("orderNo", inspection.getWorkOrder().getOrderNo()); event.put("status", inspection.getStatus());
        outbox.enqueue("QUALITY_INSPECTION", String.valueOf(inspection.getId()), "QUALITY_INSPECTION_COMPLETED", event);
        return new InspectionView(repository.save(inspection));
    }

    private void addItems(QualityInspection inspection, List<InspectionItemRequest> sources, boolean finalResult) {
        if (sources == null) return;
        int line = 1;
        for (InspectionItemRequest source : sources) {
            QualityInspectionItem item = new QualityInspectionItem();
            item.setLineNo(line++);
            item.setItemCode(source.getItemCode().trim());
            item.setItemName(source.getItemName().trim());
            item.setSpecification(trim(source.getSpecification()));
            item.setMinValue(source.getMinValue());
            item.setMaxValue(source.getMaxValue());
            item.setMeasuredValue(trim(source.getMeasuredValue()));
            item.setUnit(trim(source.getUnit()));
            item.setResult(finalResult ? itemResult(source) : "PENDING");
            item.setRemark(trim(source.getRemark()));
            inspection.addItem(item);
        }
    }

    private String itemResult(InspectionItemRequest source) {
        if (hasText(source.getResult())) {
            String result = source.getResult().trim().toUpperCase(Locale.ROOT);
            if ("PASS".equals(result) || "FAIL".equals(result)) return result;
            throw new BizException(4003, "error.validation");
        }
        if (hasText(source.getMeasuredValue()) && (source.getMinValue() != null || source.getMaxValue() != null)) {
            try {
                BigDecimal measured = new BigDecimal(source.getMeasuredValue().trim());
                if (source.getMinValue() != null && measured.compareTo(source.getMinValue()) < 0) return "FAIL";
                if (source.getMaxValue() != null && measured.compareTo(source.getMaxValue()) > 0) return "FAIL";
                return "PASS";
            } catch (NumberFormatException ex) {
                throw new BizException(4003, "error.validation");
            }
        }
        throw new BizException(4003, "execution.item-result-required");
    }

    private static String normalizeType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new BizException(4003, "error.validation");
        return type;
    }
    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private static String trim(String value) { return hasText(value) ? value.trim() : null; }
}
