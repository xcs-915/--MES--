package com.tns.mes.quality.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "qa_batch")
public class Batch extends AuditedEntity {
    @Column(name = "batch_no", nullable = false, length = 64)
    private String batchNo;
    @Column(name = "product_code", length = 64)
    private String productCode;
    @Column(name = "product_name", length = 200)
    private String productName;
    @Column(length = 20)
    private String plant;
    @Column(name = "batch_status", length = 20)
    private String batchStatus;
    @Column(name = "availability_date")
    private LocalDate availabilityDate;
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    @Column(name = "shelf_life_expiration_date")
    private LocalDate shelfLifeExpirationDate;
    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;
    @Column(name = "supplier_batch", length = 64)
    private String supplierBatch;
    @Column(length = 64)
    private String vendor;
    @Column(precision = 18, scale = 6)
    private BigDecimal quantity;
    @Column(length = 20)
    private String unit;
    @Column(name = "restricted_use", nullable = false)
    private Boolean restrictedUse = false;
    @Column(name = "inspection_lot", length = 64)
    private String inspectionLot;
    @Column(name = "inspection_status", length = 20)
    private String inspectionStatus;
    @Column(name = "batch_class", length = 40)
    private String batchClass;
    @Column(length = 1000)
    private String remark;
    @Column(nullable = false, length = 30)
    private String source = "SAP";
    @Column(name = "sap_created_at")
    private LocalDateTime sapCreatedAt;
    @Column(name = "sap_changed_at")
    private LocalDateTime sapChangedAt;
    @Column(name = "sap_last_sync_at")
    private LocalDateTime sapLastSyncAt;
    @Column(name = "sap_payload", columnDefinition = "nvarchar(max)")
    private String sapPayload;
    @Column(name = "batch_marked_for_deletion")
    private Boolean batchMarkedForDeletion;
    @Column(name = "country_of_origin", length = 3)
    private String countryOfOrigin;
    @Column(name = "region_of_origin", length = 3)
    private String regionOfOrigin;
    @Column(name = "free_defined_date1")
    private LocalDate freeDefinedDate1;
    @Column(name = "free_defined_date2")
    private LocalDate freeDefinedDate2;
    @Column(name = "free_defined_date3")
    private LocalDate freeDefinedDate3;
    @Column(name = "free_defined_date4")
    private LocalDate freeDefinedDate4;
    @Column(name = "free_defined_date5")
    private LocalDate freeDefinedDate5;
    @Column(name = "free_defined_date6")
    private LocalDate freeDefinedDate6;
    @Column(name = "batch_ext_whse_mgmt_internal_id")
    private java.util.UUID batchExtWhseMgmtInternalId;
    @Column(name = "next_inspection_date")
    private LocalDate nextInspectionDate;
    @Column(name = "last_goods_receipt_date")
    private LocalDate lastGoodsReceiptDate;
    @Column(name = "export_import_product_group", length = 4)
    private String exportImportProductGroup;
    @Column(name = "batch_certification_date")
    private LocalDate batchCertificationDate;
    @Column(name = "material", length = 18)
    private String material;
    @Column(name = "batch_identifying_plant", length = 4)
    private String batchIdentifyingPlant;

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getPlant() { return plant; }
    public void setPlant(String plant) { this.plant = plant; }
    public String getBatchStatus() { return batchStatus; }
    public void setBatchStatus(String batchStatus) { this.batchStatus = batchStatus; }
    public LocalDate getAvailabilityDate() { return availabilityDate; }
    public void setAvailabilityDate(LocalDate availabilityDate) { this.availabilityDate = availabilityDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public LocalDate getShelfLifeExpirationDate() { return shelfLifeExpirationDate; }
    public void setShelfLifeExpirationDate(LocalDate shelfLifeExpirationDate) { this.shelfLifeExpirationDate = shelfLifeExpirationDate; }
    public LocalDate getManufactureDate() { return manufactureDate; }
    public void setManufactureDate(LocalDate manufactureDate) { this.manufactureDate = manufactureDate; }
    public String getSupplierBatch() { return supplierBatch; }
    public void setSupplierBatch(String supplierBatch) { this.supplierBatch = supplierBatch; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Boolean getRestrictedUse() { return restrictedUse; }
    public void setRestrictedUse(Boolean restrictedUse) { this.restrictedUse = restrictedUse; }
    public String getInspectionLot() { return inspectionLot; }
    public void setInspectionLot(String inspectionLot) { this.inspectionLot = inspectionLot; }
    public String getInspectionStatus() { return inspectionStatus; }
    public void setInspectionStatus(String inspectionStatus) { this.inspectionStatus = inspectionStatus; }
    public String getBatchClass() { return batchClass; }
    public void setBatchClass(String batchClass) { this.batchClass = batchClass; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getSapCreatedAt() { return sapCreatedAt; }
    public void setSapCreatedAt(LocalDateTime sapCreatedAt) { this.sapCreatedAt = sapCreatedAt; }
    public LocalDateTime getSapChangedAt() { return sapChangedAt; }
    public void setSapChangedAt(LocalDateTime sapChangedAt) { this.sapChangedAt = sapChangedAt; }
    public LocalDateTime getSapLastSyncAt() { return sapLastSyncAt; }
    public void setSapLastSyncAt(LocalDateTime sapLastSyncAt) { this.sapLastSyncAt = sapLastSyncAt; }
    public String getSapPayload() { return sapPayload; }
    public void setSapPayload(String sapPayload) { this.sapPayload = sapPayload; }
    public Boolean getBatchMarkedForDeletion() { return batchMarkedForDeletion; }
    public void setBatchMarkedForDeletion(Boolean batchMarkedForDeletion) { this.batchMarkedForDeletion = batchMarkedForDeletion; }
    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }
    public String getRegionOfOrigin() { return regionOfOrigin; }
    public void setRegionOfOrigin(String regionOfOrigin) { this.regionOfOrigin = regionOfOrigin; }
    public LocalDate getFreeDefinedDate1() { return freeDefinedDate1; }
    public void setFreeDefinedDate1(LocalDate freeDefinedDate1) { this.freeDefinedDate1 = freeDefinedDate1; }
    public LocalDate getFreeDefinedDate2() { return freeDefinedDate2; }
    public void setFreeDefinedDate2(LocalDate freeDefinedDate2) { this.freeDefinedDate2 = freeDefinedDate2; }
    public LocalDate getFreeDefinedDate3() { return freeDefinedDate3; }
    public void setFreeDefinedDate3(LocalDate freeDefinedDate3) { this.freeDefinedDate3 = freeDefinedDate3; }
    public LocalDate getFreeDefinedDate4() { return freeDefinedDate4; }
    public void setFreeDefinedDate4(LocalDate freeDefinedDate4) { this.freeDefinedDate4 = freeDefinedDate4; }
    public LocalDate getFreeDefinedDate5() { return freeDefinedDate5; }
    public void setFreeDefinedDate5(LocalDate freeDefinedDate5) { this.freeDefinedDate5 = freeDefinedDate5; }
    public LocalDate getFreeDefinedDate6() { return freeDefinedDate6; }
    public void setFreeDefinedDate6(LocalDate freeDefinedDate6) { this.freeDefinedDate6 = freeDefinedDate6; }
    public java.util.UUID getBatchExtWhseMgmtInternalId() { return batchExtWhseMgmtInternalId; }
    public void setBatchExtWhseMgmtInternalId(java.util.UUID batchExtWhseMgmtInternalId) { this.batchExtWhseMgmtInternalId = batchExtWhseMgmtInternalId; }
    public LocalDate getNextInspectionDate() { return nextInspectionDate; }
    public void setNextInspectionDate(LocalDate nextInspectionDate) { this.nextInspectionDate = nextInspectionDate; }
    public LocalDate getLastGoodsReceiptDate() { return lastGoodsReceiptDate; }
    public void setLastGoodsReceiptDate(LocalDate lastGoodsReceiptDate) { this.lastGoodsReceiptDate = lastGoodsReceiptDate; }
    public String getExportImportProductGroup() { return exportImportProductGroup; }
    public void setExportImportProductGroup(String exportImportProductGroup) { this.exportImportProductGroup = exportImportProductGroup; }
    public LocalDate getBatchCertificationDate() { return batchCertificationDate; }
    public void setBatchCertificationDate(LocalDate batchCertificationDate) { this.batchCertificationDate = batchCertificationDate; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getBatchIdentifyingPlant() { return batchIdentifyingPlant; }
    public void setBatchIdentifyingPlant(String batchIdentifyingPlant) { this.batchIdentifyingPlant = batchIdentifyingPlant; }
}
