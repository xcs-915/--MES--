package com.tns.mes.execution.domain;

import com.tns.mes.common.domain.AuditedEntity;
import com.tns.mes.production.domain.WorkOrder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wh_goods_receipt")
public class GoodsReceipt extends AuditedEntity {
    @Column(name = "receipt_no", nullable = false, unique = true, length = 64)
    private String receiptNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_report_id")
    private WorkReport workReport;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private QualityInspection inspection;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;
    @Column(nullable = false, length = 20)
    private String unit;
    @Column(name = "batch_no", length = 64)
    private String batchNo;
    @Column(name = "warehouse_code", nullable = false, length = 40)
    private String warehouseCode;
    @Column(name = "storage_location", nullable = false, length = 40)
    private String storageLocation;
    @Column(name = "movement_type", nullable = false, length = 20)
    private String movementType = "1101P";
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";
    @Column(name = "operator_code", nullable = false, length = 64)
    private String operatorCode;
    @Column(name = "posting_date", nullable = false)
    private Instant postingDate;
    @Column(name = "external_doc_id", length = 80)
    private String externalDocId;
    @Column(name = "external_message", length = 1000)
    private String externalMessage;
    @Column(name = "posted_at")
    private Instant postedAt;
    @Column(length = 1000)
    private String remark;

    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String value) { receiptNo = value; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder value) { workOrder = value; }
    public WorkReport getWorkReport() { return workReport; }
    public void setWorkReport(WorkReport value) { workReport = value; }
    public QualityInspection getInspection() { return inspection; }
    public void setInspection(QualityInspection value) { inspection = value; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { quantity = value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String value) { batchNo = value; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String value) { warehouseCode = value; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String value) { storageLocation = value; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String value) { movementType = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getOperatorCode() { return operatorCode; }
    public void setOperatorCode(String value) { operatorCode = value; }
    public Instant getPostingDate() { return postingDate; }
    public void setPostingDate(Instant value) { postingDate = value; }
    public String getExternalDocId() { return externalDocId; }
    public void setExternalDocId(String value) { externalDocId = value; }
    public String getExternalMessage() { return externalMessage; }
    public void setExternalMessage(String value) { externalMessage = value; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant value) { postedAt = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
}
