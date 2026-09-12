package com.tns.mes.execution.domain;

import com.tns.mes.common.domain.AuditedEntity;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.domain.WorkOrderOperation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "prd_work_report")
public class WorkReport extends AuditedEntity {
    @Column(name = "report_no", nullable = false, unique = true, length = 64)
    private String reportNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private WorkOrderOperation operation;
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";
    @Column(name = "qualified_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal qualifiedQuantity;
    @Column(name = "unqualified_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal unqualifiedQuantity;
    @Column(name = "operator_code", nullable = false, length = 64)
    private String operatorCode;
    @Column(name = "shift_code", length = 40)
    private String shiftCode;
    @Column(name = "equipment_code", length = 64)
    private String equipmentCode;
    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;
    @Column(name = "confirmation_group", length = 80)
    private String confirmationGroup;
    @Column(name = "external_doc_id", length = 80)
    private String externalDocId;
    @Column(name = "external_message", length = 1000)
    private String externalMessage;
    @Column(name = "posted_at")
    private Instant postedAt;
    @Column(length = 1000)
    private String remark;

    public String getReportNo() { return reportNo; }
    public void setReportNo(String value) { reportNo = value; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder value) { workOrder = value; }
    public WorkOrderOperation getOperation() { return operation; }
    public void setOperation(WorkOrderOperation value) { operation = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public BigDecimal getQualifiedQuantity() { return qualifiedQuantity; }
    public void setQualifiedQuantity(BigDecimal value) { qualifiedQuantity = value; }
    public BigDecimal getUnqualifiedQuantity() { return unqualifiedQuantity; }
    public void setUnqualifiedQuantity(BigDecimal value) { unqualifiedQuantity = value; }
    public String getOperatorCode() { return operatorCode; }
    public void setOperatorCode(String value) { operatorCode = value; }
    public String getShiftCode() { return shiftCode; }
    public void setShiftCode(String value) { shiftCode = value; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String value) { equipmentCode = value; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant value) { reportedAt = value; }
    public String getConfirmationGroup() { return confirmationGroup; }
    public void setConfirmationGroup(String value) { confirmationGroup = value; }
    public String getExternalDocId() { return externalDocId; }
    public void setExternalDocId(String value) { externalDocId = value; }
    public String getExternalMessage() { return externalMessage; }
    public void setExternalMessage(String value) { externalMessage = value; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant value) { postedAt = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
}
