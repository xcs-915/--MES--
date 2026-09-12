package com.tns.mes.execution.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "prd_material_issue_item")
public class MaterialIssueItem extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private MaterialIssue issue;
    @Column(name = "line_no", nullable = false)
    private Integer lineNo;
    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;
    @Column(name = "product_name", length = 200)
    private String productName;
    @Column(name = "batch_no", length = 64)
    private String batchNo;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;
    @Column(nullable = false, length = 20)
    private String unit;
    @Column(name = "storage_location", length = 40)
    private String storageLocation;
    @Column(name = "reservation_no", length = 64)
    private String reservationNo;
    @Column(name = "reservation_item", length = 20)
    private String reservationItem;

    public MaterialIssue getIssue() { return issue; }
    public void setIssue(MaterialIssue value) { issue = value; }
    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer value) { lineNo = value; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String value) { productCode = value; }
    public String getProductName() { return productName; }
    public void setProductName(String value) { productName = value; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String value) { batchNo = value; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { quantity = value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String value) { storageLocation = value; }
    public String getReservationNo() { return reservationNo; }
    public void setReservationNo(String value) { reservationNo = value; }
    public String getReservationItem() { return reservationItem; }
    public void setReservationItem(String value) { reservationItem = value; }
}
