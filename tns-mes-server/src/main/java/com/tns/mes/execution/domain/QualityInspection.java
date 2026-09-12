package com.tns.mes.execution.domain;

import com.tns.mes.common.domain.AuditedEntity;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.domain.WorkOrderOperation;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qua_inspection")
public class QualityInspection extends AuditedEntity {
    @Column(name = "inspection_no", nullable = false, unique = true, length = 64)
    private String inspectionNo;
    @Column(name = "inspection_type", nullable = false, length = 30)
    private String inspectionType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private WorkOrderOperation operation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_report_id")
    private WorkReport workReport;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(name = "sample_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal sampleQuantity;
    @Column(name = "qualified_quantity", precision = 18, scale = 6)
    private BigDecimal qualifiedQuantity;
    @Column(name = "unqualified_quantity", precision = 18, scale = 6)
    private BigDecimal unqualifiedQuantity;
    @Column(name = "requested_by", nullable = false, length = 64)
    private String requestedBy;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "inspector_code", length = 64)
    private String inspectorCode;
    @Column(name = "inspected_at")
    private Instant inspectedAt;
    @Column(name = "defect_code", length = 64)
    private String defectCode;
    @Column(length = 40)
    private String disposition;
    @Column(length = 1000)
    private String remark;
    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<QualityInspectionItem> items = new ArrayList<>();

    public void addItem(QualityInspectionItem item) { item.setInspection(this); items.add(item); }
    public String getInspectionNo() { return inspectionNo; }
    public void setInspectionNo(String value) { inspectionNo = value; }
    public String getInspectionType() { return inspectionType; }
    public void setInspectionType(String value) { inspectionType = value; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder value) { workOrder = value; }
    public WorkOrderOperation getOperation() { return operation; }
    public void setOperation(WorkOrderOperation value) { operation = value; }
    public WorkReport getWorkReport() { return workReport; }
    public void setWorkReport(WorkReport value) { workReport = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public BigDecimal getSampleQuantity() { return sampleQuantity; }
    public void setSampleQuantity(BigDecimal value) { sampleQuantity = value; }
    public BigDecimal getQualifiedQuantity() { return qualifiedQuantity; }
    public void setQualifiedQuantity(BigDecimal value) { qualifiedQuantity = value; }
    public BigDecimal getUnqualifiedQuantity() { return unqualifiedQuantity; }
    public void setUnqualifiedQuantity(BigDecimal value) { unqualifiedQuantity = value; }
    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String value) { requestedBy = value; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant value) { requestedAt = value; }
    public String getInspectorCode() { return inspectorCode; }
    public void setInspectorCode(String value) { inspectorCode = value; }
    public Instant getInspectedAt() { return inspectedAt; }
    public void setInspectedAt(Instant value) { inspectedAt = value; }
    public String getDefectCode() { return defectCode; }
    public void setDefectCode(String value) { defectCode = value; }
    public String getDisposition() { return disposition; }
    public void setDisposition(String value) { disposition = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
    public List<QualityInspectionItem> getItems() { return items; }
}
