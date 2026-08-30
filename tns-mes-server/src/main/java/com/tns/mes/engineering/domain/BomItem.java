package com.tns.mes.engineering.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "eng_bom_item")
public class BomItem extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_id", nullable = false)
    private Bom bom;
    @Column(name = "component_product_id", nullable = false)
    private Long componentProductId;
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;
    @Column(name = "scrap_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal scrapRate = BigDecimal.ZERO;
    @Column(nullable = false, length = 20)
    private String unit = "PCS";
    @Column(name = "issue_method", nullable = false, length = 20)
    private String issueMethod = "BACKFLUSH";
    @Column(name = "reservation_no", length = 40)
    private String reservationNo;
    @Column(name = "reservation_item", length = 20)
    private String reservationItem;
    @Column(name = "operation_code", length = 40)
    private String operationCode;
    @Column(name = "material_group", length = 40)
    private String materialGroup;
    @Column(name = "requirement_date")
    private LocalDate requirementDate;
    @Column(name = "withdrawn_quantity", precision = 18, scale = 6)
    private BigDecimal withdrawnQuantity;
    @Column(name = "available_quantity", precision = 18, scale = 6)
    private BigDecimal availableQuantity;
    @Column(name = "storage_location", length = 20)
    private String storageLocation;
    @Column(name = "batch", length = 40)
    private String batch;
    @Column(name = "goods_movement_type", length = 20)
    private String goodsMovementType;
    @Column(name = "bom_item_number", length = 20)
    private String bomItemNumber;
    @Column(name = "item_description", length = 500)
    private String itemDescription;
    @Column(name = "is_bulk_material", nullable = false)
    private Boolean bulkMaterial = false;
    @Column(name = "is_backflush", nullable = false)
    private Boolean backflush = false;
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;
    @Column(name = "sap_payload", columnDefinition = "nvarchar(max)")
    private String sapPayload;
    @Column(name = "component_plant", length = 4)
    private String componentPlant;
    @Column(name = "manufacturing_order", length = 12)
    private String manufacturingOrder;
    @Column(name = "manufacturing_order_sequence", length = 6)
    private String manufacturingOrderSequence;
    @Column(name = "manufacturing_order_operation", length = 10)
    private String manufacturingOrderOperation;
    @Column(name = "production_plant", length = 4)
    private String productionPlant;
    @Column(name = "order_internal_bill_of_operations", length = 10)
    private String orderInternalBillOfOperations;
    @Column(name = "requirement_time", length = 32)
    private String requirementTime;
    @Column(name = "reservation_finally_issued")
    private Boolean reservationFinallyIssued;
    @Column(name = "cost_relevant", length = 1)
    private String costRelevant;
    @Column(name = "sales_order", length = 10)
    private String salesOrder;
    @Column(name = "sales_order_item", length = 6)
    private String salesOrderItem;
    @Column(name = "sort_field", length = 10)
    private String sortField;
    @Column(name = "bom_item_category", length = 1)
    private String bomItemCategory;
    @Column(name = "supply_area", length = 20)
    private String supplyArea;
    @Column(name = "goods_recipient_name", length = 40)
    private String goodsRecipientName;
    @Column(name = "unloading_point_name", length = 25)
    private String unloadingPointName;
    @Column(name = "is_alternative_item")
    private Boolean alternativeItem;
    @Column(name = "alternative_item_group", length = 2)
    private String alternativeItemGroup;
    @Column(name = "alternative_item_strategy", length = 1)
    private String alternativeItemStrategy;
    @Column(name = "alternative_item_priority", length = 2)
    private String alternativeItemPriority;
    @Column(name = "usage_probability_percent", precision = 3, scale = 0)
    private BigDecimal usageProbabilityPercent;
    @Column(name = "is_phantom_item")
    private Boolean phantomItem;
    @Column(name = "lead_time_offset", precision = 3, scale = 0)
    private BigDecimal leadTimeOffset;
    @Column(name = "quantity_is_fixed")
    private Boolean quantityIsFixed;
    @Column(name = "is_net_scrap")
    private Boolean netScrap;
    @Column(name = "component_scrap_in_percent", precision = 5, scale = 2)
    private BigDecimal componentScrapInPercent;
    @Column(name = "operation_scrap_in_percent", precision = 5, scale = 2)
    private BigDecimal operationScrapInPercent;
    @Column(name = "original_quantity", precision = 13, scale = 3)
    private BigDecimal originalQuantity;
    @Column(name = "entry_unit", length = 3)
    private String entryUnit;
    @Column(name = "goods_movement_entry_qty", precision = 13, scale = 3)
    private BigDecimal goodsMovementEntryQty;
    @Column(name = "batch_split_type", length = 1)
    private String batchSplitType;
    @Column(name = "base_unit_iso_code", length = 3)
    private String baseUnitIsoCode;
    @Column(name = "base_unit_sap_code", length = 3)
    private String baseUnitSapCode;
    @Column(name = "entry_unit_iso_code", length = 3)
    private String entryUnitIsoCode;
    @Column(name = "entry_unit_sap_code", length = 3)
    private String entryUnitSapCode;
    @Column(length = 5)
    private String currency;
    @Column(name = "withdrawn_quantity_amount", precision = 14, scale = 3)
    private BigDecimal withdrawnQuantityAmount;

    public Bom getBom() { return bom; }
    public void setBom(Bom bom) { this.bom = bom; }
    public Long getComponentProductId() { return componentProductId; }
    public void setComponentProductId(Long componentProductId) { this.componentProductId = componentProductId; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getScrapRate() { return scrapRate; }
    public void setScrapRate(BigDecimal scrapRate) { this.scrapRate = scrapRate; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getIssueMethod() { return issueMethod; }
    public void setIssueMethod(String issueMethod) { this.issueMethod = issueMethod; }
    public String getReservationNo() { return reservationNo; }
    public void setReservationNo(String value) { reservationNo = value; }
    public String getReservationItem() { return reservationItem; }
    public void setReservationItem(String value) { reservationItem = value; }
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String value) { operationCode = value; }
    public String getMaterialGroup() { return materialGroup; }
    public void setMaterialGroup(String value) { materialGroup = value; }
    public LocalDate getRequirementDate() { return requirementDate; }
    public void setRequirementDate(LocalDate value) { requirementDate = value; }
    public BigDecimal getWithdrawnQuantity() { return withdrawnQuantity; }
    public void setWithdrawnQuantity(BigDecimal value) { withdrawnQuantity = value; }
    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(BigDecimal value) { availableQuantity = value; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String value) { storageLocation = value; }
    public String getBatch() { return batch; }
    public void setBatch(String value) { batch = value; }
    public String getGoodsMovementType() { return goodsMovementType; }
    public void setGoodsMovementType(String value) { goodsMovementType = value; }
    public String getBomItemNumber() { return bomItemNumber; }
    public void setBomItemNumber(String value) { bomItemNumber = value; }
    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String value) { itemDescription = value; }
    public Boolean getBulkMaterial() { return bulkMaterial; }
    public void setBulkMaterial(Boolean value) { bulkMaterial = value; }
    public Boolean getBackflush() { return backflush; }
    public void setBackflush(Boolean value) { backflush = value; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean value) { deleted = value; }
    public String getSapPayload() { return sapPayload; }
    public void setSapPayload(String value) { sapPayload = value; }
    public String getComponentPlant() { return componentPlant; }
    public void setComponentPlant(String value) { componentPlant = value; }
    public String getManufacturingOrder() { return manufacturingOrder; }
    public void setManufacturingOrder(String value) { manufacturingOrder = value; }
    public String getManufacturingOrderSequence() { return manufacturingOrderSequence; }
    public void setManufacturingOrderSequence(String value) { manufacturingOrderSequence = value; }
    public String getManufacturingOrderOperation() { return manufacturingOrderOperation; }
    public void setManufacturingOrderOperation(String value) { manufacturingOrderOperation = value; }
    public String getProductionPlant() { return productionPlant; }
    public void setProductionPlant(String value) { productionPlant = value; }
    public String getOrderInternalBillOfOperations() { return orderInternalBillOfOperations; }
    public void setOrderInternalBillOfOperations(String value) { orderInternalBillOfOperations = value; }
    public String getRequirementTime() { return requirementTime; }
    public void setRequirementTime(String value) { requirementTime = value; }
    public Boolean getReservationFinallyIssued() { return reservationFinallyIssued; }
    public void setReservationFinallyIssued(Boolean value) { reservationFinallyIssued = value; }
    public String getCostRelevant() { return costRelevant; }
    public void setCostRelevant(String value) { costRelevant = value; }
    public String getSalesOrder() { return salesOrder; }
    public void setSalesOrder(String value) { salesOrder = value; }
    public String getSalesOrderItem() { return salesOrderItem; }
    public void setSalesOrderItem(String value) { salesOrderItem = value; }
    public String getSortField() { return sortField; }
    public void setSortField(String value) { sortField = value; }
    public String getBomItemCategory() { return bomItemCategory; }
    public void setBomItemCategory(String value) { bomItemCategory = value; }
    public String getSupplyArea() { return supplyArea; }
    public void setSupplyArea(String value) { supplyArea = value; }
    public String getGoodsRecipientName() { return goodsRecipientName; }
    public void setGoodsRecipientName(String value) { goodsRecipientName = value; }
    public String getUnloadingPointName() { return unloadingPointName; }
    public void setUnloadingPointName(String value) { unloadingPointName = value; }
    public Boolean getAlternativeItem() { return alternativeItem; }
    public void setAlternativeItem(Boolean value) { alternativeItem = value; }
    public String getAlternativeItemGroup() { return alternativeItemGroup; }
    public void setAlternativeItemGroup(String value) { alternativeItemGroup = value; }
    public String getAlternativeItemStrategy() { return alternativeItemStrategy; }
    public void setAlternativeItemStrategy(String value) { alternativeItemStrategy = value; }
    public String getAlternativeItemPriority() { return alternativeItemPriority; }
    public void setAlternativeItemPriority(String value) { alternativeItemPriority = value; }
    public BigDecimal getUsageProbabilityPercent() { return usageProbabilityPercent; }
    public void setUsageProbabilityPercent(BigDecimal value) { usageProbabilityPercent = value; }
    public Boolean getPhantomItem() { return phantomItem; }
    public void setPhantomItem(Boolean value) { phantomItem = value; }
    public BigDecimal getLeadTimeOffset() { return leadTimeOffset; }
    public void setLeadTimeOffset(BigDecimal value) { leadTimeOffset = value; }
    public Boolean getQuantityIsFixed() { return quantityIsFixed; }
    public void setQuantityIsFixed(Boolean value) { quantityIsFixed = value; }
    public Boolean getNetScrap() { return netScrap; }
    public void setNetScrap(Boolean value) { netScrap = value; }
    public BigDecimal getComponentScrapInPercent() { return componentScrapInPercent; }
    public void setComponentScrapInPercent(BigDecimal value) { componentScrapInPercent = value; }
    public BigDecimal getOperationScrapInPercent() { return operationScrapInPercent; }
    public void setOperationScrapInPercent(BigDecimal value) { operationScrapInPercent = value; }
    public BigDecimal getOriginalQuantity() { return originalQuantity; }
    public void setOriginalQuantity(BigDecimal value) { originalQuantity = value; }
    public String getEntryUnit() { return entryUnit; }
    public void setEntryUnit(String value) { entryUnit = value; }
    public BigDecimal getGoodsMovementEntryQty() { return goodsMovementEntryQty; }
    public void setGoodsMovementEntryQty(BigDecimal value) { goodsMovementEntryQty = value; }
    public String getBatchSplitType() { return batchSplitType; }
    public void setBatchSplitType(String value) { batchSplitType = value; }
    public String getBaseUnitIsoCode() { return baseUnitIsoCode; }
    public void setBaseUnitIsoCode(String value) { baseUnitIsoCode = value; }
    public String getBaseUnitSapCode() { return baseUnitSapCode; }
    public void setBaseUnitSapCode(String value) { baseUnitSapCode = value; }
    public String getEntryUnitIsoCode() { return entryUnitIsoCode; }
    public void setEntryUnitIsoCode(String value) { entryUnitIsoCode = value; }
    public String getEntryUnitSapCode() { return entryUnitSapCode; }
    public void setEntryUnitSapCode(String value) { entryUnitSapCode = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { currency = value; }
    public BigDecimal getWithdrawnQuantityAmount() { return withdrawnQuantityAmount; }
    public void setWithdrawnQuantityAmount(BigDecimal value) { withdrawnQuantityAmount = value; }
}
