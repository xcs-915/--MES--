package com.tns.mes.production.domain;

import com.tns.mes.common.domain.AuditedEntity;
import com.tns.mes.engineering.domain.ProcessOperation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "prd_work_order_operation")
public class WorkOrderOperation extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private ProcessOperation operation;
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(name = "planned_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal plannedQuantity;
    @Column(name = "completed_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal completedQuantity = BigDecimal.ZERO;
    @Column(name = "operation_code", length = 40)
    private String operationCode;
    @Column(name = "operation_name", length = 200)
    private String operationName;
    @Column(name = "work_center_code", length = 40)
    private String workCenterCode;
    @Column(name = "work_center_desc", length = 200)
    private String workCenterDesc;
    @Column(length = 4)
    private String plant;
    @Column(name = "control_key", length = 40)
    private String controlKey;
    @Column(name = "operation_unit", length = 20)
    private String operationUnit;
    @Column(name = "planned_yield_quantity", precision = 18, scale = 6)
    private BigDecimal plannedYieldQuantity;
    @Column(name = "confirmed_yield_quantity", precision = 18, scale = 6)
    private BigDecimal confirmedYieldQuantity;
    @Column(name = "planned_total_quantity", precision = 18, scale = 6)
    private BigDecimal plannedTotalQuantity;
    @Column(name = "work_center_internal_id", length = 40)
    private String workCenterInternalId;
    @Column(name = "standard_time_seconds")
    private Integer standardTimeSeconds;
    @Column(name = "sap_payload", columnDefinition = "nvarchar(max)")
    private String sapPayload;

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public ProcessOperation getOperation() { return operation; }
    public void setOperation(ProcessOperation operation) { this.operation = operation; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPlannedQuantity() { return plannedQuantity; }
    public void setPlannedQuantity(BigDecimal plannedQuantity) { this.plannedQuantity = plannedQuantity; }
    public BigDecimal getCompletedQuantity() { return completedQuantity; }
    public void setCompletedQuantity(BigDecimal completedQuantity) { this.completedQuantity = completedQuantity; }
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String value) { operationCode = value; }
    public String getOperationName() { return operationName; }
    public void setOperationName(String value) { operationName = value; }
    public String getWorkCenterCode() { return workCenterCode; }
    public void setWorkCenterCode(String value) { workCenterCode = value; }
    public String getWorkCenterDesc() { return workCenterDesc; }
    public void setWorkCenterDesc(String value) { workCenterDesc = value; }
    public String getPlant() { return plant; }
    public void setPlant(String value) { plant = value; }
    public String getControlKey() { return controlKey; }
    public void setControlKey(String value) { controlKey = value; }
    public String getOperationUnit() { return operationUnit; }
    public void setOperationUnit(String value) { operationUnit = value; }
    public BigDecimal getPlannedYieldQuantity() { return plannedYieldQuantity; }
    public void setPlannedYieldQuantity(BigDecimal value) { plannedYieldQuantity = value; }
    public BigDecimal getConfirmedYieldQuantity() { return confirmedYieldQuantity; }
    public void setConfirmedYieldQuantity(BigDecimal value) { confirmedYieldQuantity = value; }
    public BigDecimal getPlannedTotalQuantity() { return plannedTotalQuantity; }
    public void setPlannedTotalQuantity(BigDecimal value) { plannedTotalQuantity = value; }
    public String getWorkCenterInternalId() { return workCenterInternalId; }
    public void setWorkCenterInternalId(String value) { workCenterInternalId = value; }
    public Integer getStandardTimeSeconds() { return standardTimeSeconds; }
    public void setStandardTimeSeconds(Integer value) { standardTimeSeconds = value; }
    public String getSapPayload() { return sapPayload; }
    public void setSapPayload(String value) { sapPayload = value; }
}

