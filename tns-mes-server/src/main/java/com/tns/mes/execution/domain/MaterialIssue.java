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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prd_material_issue")
public class MaterialIssue extends AuditedEntity {
    @Column(name = "issue_no", nullable = false, unique = true, length = 64)
    private String issueNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id")
    private WorkOrderOperation operation;
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";
    @Column(name = "posting_date", nullable = false)
    private Instant postingDate;
    @Column(name = "warehouse_code", length = 40)
    private String warehouseCode;
    @Column(name = "operator_code", nullable = false, length = 64)
    private String operatorCode;
    @Column(name = "external_doc_id", length = 80)
    private String externalDocId;
    @Column(name = "external_message", length = 1000)
    private String externalMessage;
    @Column(name = "posted_at")
    private Instant postedAt;
    @Column(length = 1000)
    private String remark;
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<MaterialIssueItem> items = new ArrayList<>();

    public void addItem(MaterialIssueItem item) { item.setIssue(this); items.add(item); }
    public String getIssueNo() { return issueNo; }
    public void setIssueNo(String value) { issueNo = value; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder value) { workOrder = value; }
    public WorkOrderOperation getOperation() { return operation; }
    public void setOperation(WorkOrderOperation value) { operation = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Instant getPostingDate() { return postingDate; }
    public void setPostingDate(Instant value) { postingDate = value; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String value) { warehouseCode = value; }
    public String getOperatorCode() { return operatorCode; }
    public void setOperatorCode(String value) { operatorCode = value; }
    public String getExternalDocId() { return externalDocId; }
    public void setExternalDocId(String value) { externalDocId = value; }
    public String getExternalMessage() { return externalMessage; }
    public void setExternalMessage(String value) { externalMessage = value; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant value) { postedAt = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
    public List<MaterialIssueItem> getItems() { return items; }
}
