package com.tns.mes.production.domain;

import com.tns.mes.common.domain.AuditedEntity;
import com.tns.mes.engineering.domain.Bom;
import com.tns.mes.engineering.domain.ProcessRoute;
import com.tns.mes.engineering.domain.Product;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prd_work_order")
public class WorkOrder extends AuditedEntity {
    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id")
    private Bom bom;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private ProcessRoute route;
    @Column(name = "factory_id")
    private Long factoryId;
    @Column(name = "workshop_id")
    private Long workshopId;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;
    @Column(name = "completed_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal completedQuantity = BigDecimal.ZERO;
    @Column(nullable = false)
    private Integer priority = 50;
    @Column(name = "planned_start")
    private LocalDateTime plannedStart;
    @Column(name = "planned_end")
    private LocalDateTime plannedEnd;
    @Column(nullable = false, length = 20)
    private String status = WorkOrderStatus.DRAFT.name();
    @Column(nullable = false, length = 30)
    private String source = "MANUAL";
    @Column(length = 1000)
    private String remark;
    @Column(name = "order_category", length = 20)
    private String orderCategory;
    @Column(name = "order_type", length = 20)
    private String orderType;
    @Column(name = "production_plant", length = 20)
    private String productionPlant;
    @Column(length = 20)
    private String plant;
    @Column(name = "storage_location", length = 20)
    private String storageLocation;
    @Column(name = "mrp_area", length = 20)
    private String mrpArea;
    @Column(name = "mrp_controller", length = 20)
    private String mrpController;
    @Column(name = "production_supervisor", length = 20)
    private String productionSupervisor;
    @Column(name = "production_version", length = 40)
    private String productionVersion;
    @Column(name = "planned_order", length = 64)
    private String plannedOrder;
    @Column(name = "sales_order", length = 64)
    private String salesOrder;
    @Column(name = "sales_order_item", length = 20)
    private String salesOrderItem;
    @Column(name = "company_code", length = 20)
    private String companyCode;
    @Column(name = "profit_center", length = 40)
    private String profitCenter;
    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;
    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;
    @Column(name = "actual_release_date")
    private LocalDate actualReleaseDate;
    @Column(name = "production_unit", length = 20)
    private String productionUnit;
    @Column(name = "planned_scrap_quantity", precision = 18, scale = 6)
    private BigDecimal plannedScrapQuantity;
    @Column(name = "confirmed_yield_quantity", precision = 18, scale = 6)
    private BigDecimal confirmedYieldQuantity;
    @Column(name = "order_long_text", length = 2000)
    private String orderLongText;
    @Column(nullable = false)
    private Boolean locked = false;
    @Column(name = "marked_for_deletion", nullable = false)
    private Boolean markedForDeletion = false;
    @Column(name = "sap_created_at")
    private LocalDateTime sapCreatedAt;
    @Column(name = "sap_changed_at")
    private LocalDateTime sapChangedAt;
    @Column(name = "sap_last_sync_at")
    private LocalDateTime sapLastSyncAt;
    @Column(name = "sap_payload", columnDefinition = "nvarchar(max)")
    private String sapPayload;

    // Planned order + SAP extended fields
    @Column(name = "planned_order_type", length = 4)
    private String plannedOrderType;
    @Column(name = "planned_order_profile", length = 4)
    private String plannedOrderProfile;
    @Column(name = "material_name", length = 40)
    private String materialName;
    @Column(name = "mrp_plant", length = 4)
    private String mrpPlant;
    @Column(name = "material_procurement_category", length = 1)
    private String materialProcurementCategory;
    @Column(name = "material_procurement_type", length = 1)
    private String materialProcurementType;
    @Column(name = "planned_scrap_qty_sap", precision = 13, scale = 3)
    private BigDecimal plannedScrapQtySap;
    @Column(name = "goods_receipt_qty", precision = 13, scale = 3)
    private BigDecimal goodsReceiptQty;
    @Column(name = "issued_quantity", precision = 13, scale = 3)
    private BigDecimal issuedQuantity;
    @Column(name = "planned_order_opening_date")
    private LocalDateTime plannedOrderOpeningDate;
    @Column(name = "production_start_date")
    private LocalDateTime productionStartDate;
    @Column(name = "production_end_date")
    private LocalDateTime productionEndDate;
    @Column(length = 10)
    private String customer;
    @Column(name = "wbs_element_internal_id", length = 24)
    private String wbsElementInternalId;
    @Column(name = "wbs_element", length = 24)
    private String wbsElement;
    @Column(name = "wbs_description", length = 40)
    private String wbsDescription;
    @Column(name = "account_assignment_category", length = 1)
    private String accountAssignmentCategory;
    @Column(name = "purchasing_group", length = 3)
    private String purchasingGroup;
    @Column(name = "purchasing_organization", length = 4)
    private String purchasingOrganization;
    @Column(name = "fixed_supplier", length = 10)
    private String fixedSupplier;
    @Column(name = "purchasing_document", length = 10)
    private String purchasingDocument;
    @Column(name = "purchasing_document_item", length = 5)
    private String purchasingDocumentItem;
    @Column(name = "quota_arrangement", length = 10)
    private String quotaArrangement;
    @Column(name = "quota_arrangement_item", length = 3)
    private String quotaArrangementItem;
    @Column(name = "supplier_name", length = 80)
    private String supplierName;
    @Column(name = "planned_order_is_firm")
    private Boolean plannedOrderIsFirm;
    @Column(name = "planned_order_is_convertible")
    private Boolean plannedOrderIsConvertible;
    @Column(name = "planned_order_bom_is_fixed")
    private Boolean plannedOrderBomIsFixed;
    @Column(name = "planned_order_capacity_is_dsptchd")
    private Boolean plannedOrderCapacityIsDsptchd;
    @Column(name = "capacity_requirement", length = 12)
    private String capacityRequirement;
    @Column(name = "capacity_requirement_origin", length = 1)
    private String capacityRequirementOrigin;
    @Column(name = "bill_of_operations_type", length = 1)
    private String billOfOperationsType;
    @Column(name = "bill_of_operations_group", length = 8)
    private String billOfOperationsGroup;
    @Column(name = "bill_of_operations", length = 2)
    private String billOfOperations;
    @Column(name = "last_scheduled_date")
    private LocalDateTime lastScheduledDate;
    @Column(name = "scheduling_type", length = 1)
    private String schedulingType;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkOrderOperation> operations = new ArrayList<>();

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Bom getBom() { return bom; }
    public void setBom(Bom bom) { this.bom = bom; }
    public ProcessRoute getRoute() { return route; }
    public void setRoute(ProcessRoute route) { this.route = route; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }
    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getCompletedQuantity() { return completedQuantity; }
    public void setCompletedQuantity(BigDecimal completedQuantity) { this.completedQuantity = completedQuantity; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDateTime plannedStart) { this.plannedStart = plannedStart; }
    public LocalDateTime getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDateTime plannedEnd) { this.plannedEnd = plannedEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOrderCategory() { return orderCategory; }
    public void setOrderCategory(String value) { orderCategory = value; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String value) { orderType = value; }
    public String getProductionPlant() { return productionPlant; }
    public void setProductionPlant(String value) { productionPlant = value; }
    public String getPlant() { return plant; }
    public void setPlant(String value) { plant = value; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String value) { storageLocation = value; }
    public String getMrpArea() { return mrpArea; }
    public void setMrpArea(String value) { mrpArea = value; }
    public String getMrpController() { return mrpController; }
    public void setMrpController(String value) { mrpController = value; }
    public String getProductionSupervisor() { return productionSupervisor; }
    public void setProductionSupervisor(String value) { productionSupervisor = value; }
    public String getProductionVersion() { return productionVersion; }
    public void setProductionVersion(String value) { productionVersion = value; }
    public String getPlannedOrder() { return plannedOrder; }
    public void setPlannedOrder(String value) { plannedOrder = value; }
    public String getSalesOrder() { return salesOrder; }
    public void setSalesOrder(String value) { salesOrder = value; }
    public String getSalesOrderItem() { return salesOrderItem; }
    public void setSalesOrderItem(String value) { salesOrderItem = value; }
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String value) { companyCode = value; }
    public String getProfitCenter() { return profitCenter; }
    public void setProfitCenter(String value) { profitCenter = value; }
    public LocalDateTime getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(LocalDateTime value) { scheduledStart = value; }
    public LocalDateTime getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(LocalDateTime value) { scheduledEnd = value; }
    public LocalDate getActualReleaseDate() { return actualReleaseDate; }
    public void setActualReleaseDate(LocalDate value) { actualReleaseDate = value; }
    public String getProductionUnit() { return productionUnit; }
    public void setProductionUnit(String value) { productionUnit = value; }
    public BigDecimal getPlannedScrapQuantity() { return plannedScrapQuantity; }
    public void setPlannedScrapQuantity(BigDecimal value) { plannedScrapQuantity = value; }
    public BigDecimal getConfirmedYieldQuantity() { return confirmedYieldQuantity; }
    public void setConfirmedYieldQuantity(BigDecimal value) { confirmedYieldQuantity = value; }
    public String getOrderLongText() { return orderLongText; }
    public void setOrderLongText(String value) { orderLongText = value; }
    public Boolean getLocked() { return locked; }
    public void setLocked(Boolean value) { locked = value; }
    public Boolean getMarkedForDeletion() { return markedForDeletion; }
    public void setMarkedForDeletion(Boolean value) { markedForDeletion = value; }
    public LocalDateTime getSapCreatedAt() { return sapCreatedAt; }
    public void setSapCreatedAt(LocalDateTime value) { sapCreatedAt = value; }
    public LocalDateTime getSapChangedAt() { return sapChangedAt; }
    public void setSapChangedAt(LocalDateTime value) { sapChangedAt = value; }
    public LocalDateTime getSapLastSyncAt() { return sapLastSyncAt; }
    public void setSapLastSyncAt(LocalDateTime value) { sapLastSyncAt = value; }
    public String getSapPayload() { return sapPayload; }
    public void setSapPayload(String value) { sapPayload = value; }
    public String getPlannedOrderType() { return plannedOrderType; }
    public void setPlannedOrderType(String value) { plannedOrderType = value; }
    public String getPlannedOrderProfile() { return plannedOrderProfile; }
    public void setPlannedOrderProfile(String value) { plannedOrderProfile = value; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String value) { materialName = value; }
    public String getMrpPlant() { return mrpPlant; }
    public void setMrpPlant(String value) { mrpPlant = value; }
    public String getMaterialProcurementCategory() { return materialProcurementCategory; }
    public void setMaterialProcurementCategory(String value) { materialProcurementCategory = value; }
    public String getMaterialProcurementType() { return materialProcurementType; }
    public void setMaterialProcurementType(String value) { materialProcurementType = value; }
    public BigDecimal getPlannedScrapQtySap() { return plannedScrapQtySap; }
    public void setPlannedScrapQtySap(BigDecimal value) { plannedScrapQtySap = value; }
    public BigDecimal getGoodsReceiptQty() { return goodsReceiptQty; }
    public void setGoodsReceiptQty(BigDecimal value) { goodsReceiptQty = value; }
    public BigDecimal getIssuedQuantity() { return issuedQuantity; }
    public void setIssuedQuantity(BigDecimal value) { issuedQuantity = value; }
    public LocalDateTime getPlannedOrderOpeningDate() { return plannedOrderOpeningDate; }
    public void setPlannedOrderOpeningDate(LocalDateTime value) { plannedOrderOpeningDate = value; }
    public LocalDateTime getProductionStartDate() { return productionStartDate; }
    public void setProductionStartDate(LocalDateTime value) { productionStartDate = value; }
    public LocalDateTime getProductionEndDate() { return productionEndDate; }
    public void setProductionEndDate(LocalDateTime value) { productionEndDate = value; }
    public String getCustomer() { return customer; }
    public void setCustomer(String value) { customer = value; }
    public String getWbsElementInternalId() { return wbsElementInternalId; }
    public void setWbsElementInternalId(String value) { wbsElementInternalId = value; }
    public String getWbsElement() { return wbsElement; }
    public void setWbsElement(String value) { wbsElement = value; }
    public String getWbsDescription() { return wbsDescription; }
    public void setWbsDescription(String value) { wbsDescription = value; }
    public String getAccountAssignmentCategory() { return accountAssignmentCategory; }
    public void setAccountAssignmentCategory(String value) { accountAssignmentCategory = value; }
    public String getPurchasingGroup() { return purchasingGroup; }
    public void setPurchasingGroup(String value) { purchasingGroup = value; }
    public String getPurchasingOrganization() { return purchasingOrganization; }
    public void setPurchasingOrganization(String value) { purchasingOrganization = value; }
    public String getFixedSupplier() { return fixedSupplier; }
    public void setFixedSupplier(String value) { fixedSupplier = value; }
    public String getPurchasingDocument() { return purchasingDocument; }
    public void setPurchasingDocument(String value) { purchasingDocument = value; }
    public String getPurchasingDocumentItem() { return purchasingDocumentItem; }
    public void setPurchasingDocumentItem(String value) { purchasingDocumentItem = value; }
    public String getQuotaArrangement() { return quotaArrangement; }
    public void setQuotaArrangement(String value) { quotaArrangement = value; }
    public String getQuotaArrangementItem() { return quotaArrangementItem; }
    public void setQuotaArrangementItem(String value) { quotaArrangementItem = value; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String value) { supplierName = value; }
    public Boolean getPlannedOrderIsFirm() { return plannedOrderIsFirm; }
    public void setPlannedOrderIsFirm(Boolean value) { plannedOrderIsFirm = value; }
    public Boolean getPlannedOrderIsConvertible() { return plannedOrderIsConvertible; }
    public void setPlannedOrderIsConvertible(Boolean value) { plannedOrderIsConvertible = value; }
    public Boolean getPlannedOrderBomIsFixed() { return plannedOrderBomIsFixed; }
    public void setPlannedOrderBomIsFixed(Boolean value) { plannedOrderBomIsFixed = value; }
    public Boolean getPlannedOrderCapacityIsDsptchd() { return plannedOrderCapacityIsDsptchd; }
    public void setPlannedOrderCapacityIsDsptchd(Boolean value) { plannedOrderCapacityIsDsptchd = value; }
    public String getCapacityRequirement() { return capacityRequirement; }
    public void setCapacityRequirement(String value) { capacityRequirement = value; }
    public String getCapacityRequirementOrigin() { return capacityRequirementOrigin; }
    public void setCapacityRequirementOrigin(String value) { capacityRequirementOrigin = value; }
    public String getBillOfOperationsType() { return billOfOperationsType; }
    public void setBillOfOperationsType(String value) { billOfOperationsType = value; }
    public String getBillOfOperationsGroup() { return billOfOperationsGroup; }
    public void setBillOfOperationsGroup(String value) { billOfOperationsGroup = value; }
    public String getBillOfOperations() { return billOfOperations; }
    public void setBillOfOperations(String value) { billOfOperations = value; }
    public LocalDateTime getLastScheduledDate() { return lastScheduledDate; }
    public void setLastScheduledDate(LocalDateTime value) { lastScheduledDate = value; }
    public String getSchedulingType() { return schedulingType; }
    public void setSchedulingType(String value) { schedulingType = value; }
    public List<WorkOrderOperation> getOperations() { return operations; }
    public void setOperations(List<WorkOrderOperation> operations) {
        this.operations.clear();
        if (operations != null) operations.forEach(this::addOperation);
    }
    public void addOperation(WorkOrderOperation operation) { operation.setWorkOrder(this); operations.add(operation); }
}
