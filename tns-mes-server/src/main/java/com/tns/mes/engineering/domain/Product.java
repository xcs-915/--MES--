package com.tns.mes.engineering.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eng_product")
public class Product extends AuditedEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    @Column(name = "name_zh", nullable = false, length = 200)
    private String nameZh;
    @Column(name = "name_en", length = 200)
    private String nameEn;
    @Column(name = "name_ar", length = 200)
    private String nameAr;
    @Column(name = "product_type", nullable = false, length = 40)
    private String productType = "FINISHED";
    @Column(nullable = false, length = 20)
    private String unit = "PCS";
    @Column(length = 500)
    private String specification;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(nullable = false)
    private Boolean traceable = true;
    @Column(nullable = false, length = 30)
    private String source = "SAP";
    @Column(name = "product_old_id", length = 64)
    private String productOldId;
    @Column(name = "product_group", length = 64)
    private String productGroup;
    @Column(name = "gross_weight", precision = 18, scale = 6)
    private BigDecimal grossWeight;
    @Column(name = "net_weight", precision = 18, scale = 6)
    private BigDecimal netWeight;
    @Column(name = "weight_unit", length = 20)
    private String weightUnit;
    @Column(name = "country_of_origin", length = 8)
    private String countryOfOrigin;
    @Column(name = "product_hierarchy", length = 64)
    private String productHierarchy;
    @Column(name = "division_code", length = 20)
    private String divisionCode;
    @Column(name = "manufacturer_number", length = 64)
    private String manufacturerNumber;
    @Column(name = "manufacturer_part_number", length = 100)
    private String manufacturerPartNumber;
    @Column(name = "material_revision_level", length = 40)
    private String materialRevisionLevel;
    @Column(name = "serial_number_profile", length = 40)
    private String serialNumberProfile;
    @Column(name = "batch_managed", nullable = false)
    private Boolean batchManaged = false;
    @Column(name = "marked_for_deletion", nullable = false)
    private Boolean markedForDeletion = false;
    @Column(length = 100)
    private String brand;
    @Column(length = 100)
    private String color;
    @Column(name = "customer_part_number", length = 100)
    private String customerPartNumber;
    @Column(name = "product_model", length = 100)
    private String productModel;
    @Column(name = "drawing_number", length = 100)
    private String drawingNumber;
    @Column(name = "sap_created_at")
    private LocalDateTime sapCreatedAt;
    @Column(name = "sap_changed_at")
    private LocalDateTime sapChangedAt;
    @Column(name = "sap_last_sync_at")
    private LocalDateTime sapLastSyncAt;
    @Column(name = "sap_payload", columnDefinition = "nvarchar(max)")
    private String sapPayload;
    @Column(name = "min_packaging_qty", precision = 18, scale = 6)
    private BigDecimal minPackagingQty;
    @Column(name = "procurement_type", length = 20)
    private String procurementType;
    @Column(name = "safety_stock_qty", precision = 18, scale = 6)
    private BigDecimal safetyStockQuantity;
    @Column(name = "lot_size_rounding_qty", precision = 18, scale = 6)
    private BigDecimal lotSizeRoundingQuantity;
    @Column(name = "over_delivery_tolerance", precision = 18, scale = 6)
    private BigDecimal overDeliveryTolerance;
    @Column(name = "unlimited_over_delivery", nullable = false)
    private Boolean unlimitedOverDelivery = false;
    @Column(name = "production_storage_location", length = 20)
    private String productionStorageLocation;
    @Column(name = "default_storage_location", length = 20)
    private String defaultStorageLocation;
    @Column(name = "product_description", length = 500)
    private String productDescription;
    @Column(name = "yy1_color_number", length = 25)
    private String yy1ColorNumber;
    @Column(name = "yy1_fifo_prosign", length = 18)
    private String yy1FifoProsign;
    @Column(name = "yy1_moisture_level", length = 2)
    private String yy1MoistureLevel;
    @Column(name = "yy1_moisture_sensitive", length = 1)
    private String yy1MoistureSensitive;
    @Column(name = "yy1_shape_and_size", length = 30)
    private String yy1ShapeAndSize;
    @Column(name = "yy1_material", length = 70)
    private String yy1Material;
    @Column(name = "yy1_brand_m", length = 30)
    private String yy1BrandM;
    @Column(name = "yy1_designer", length = 100)
    private String yy1Designer;
    @Column(name = "yy1_cavity", length = 100)
    private String yy1Cavity;
    @Column(name = "yy1_color_region", length = 100)
    private String yy1ColorRegion;
    @Column(name = "yy1_plm_package_number", length = 40)
    private String yy1PlmPackageNumber;
    @Column(name = "yy1_product_type_custom", length = 20)
    private String yy1ProductTypeCustom;
    @Column(name = "yy1_process_treatment", length = 30)
    private String yy1ProcessTreatment;
    @Column(name = "yy1_description_otp", length = 150)
    private String yy1DescriptionOtp;
    @Column(name = "yy1_encapsulation", length = 30)
    private String yy1Encapsulation;
    @Column(name = "yy1_project", length = 24)
    private String yy1Project;
    @Column(name = "yy1_exterior_color", length = 10)
    private String yy1ExteriorColor;
    @Column(name = "cross_plant_status", length = 2)
    private String crossPlantStatus;
    @Column(name = "cross_plant_status_validity_date")
    private LocalDateTime crossPlantStatusValidityDate;
    @Column(name = "created_by_user", length = 12)
    private String createdByUser;
    @Column(name = "last_changed_by_user", length = 12)
    private String lastChangedByUser;
    @Column(name = "purchase_order_quantity_unit", length = 3)
    private String purchaseOrderQuantityUnit;
    @Column(name = "source_of_supply", length = 1)
    private String sourceOfSupply;
    @Column(name = "competitor_id", length = 10)
    private String competitorId;
    @Column(name = "item_category_group", length = 4)
    private String itemCategoryGroup;
    @Column(name = "varbl_pur_ord_unit_is_active", length = 1)
    private String varblPurOrdUnitIsActive;
    @Column(name = "volume_unit", length = 3)
    private String volumeUnit;
    @Column(name = "material_volume", precision = 13, scale = 3)
    private BigDecimal materialVolume;
    @Column(name = "anp_code", length = 9)
    private String anpCode;
    @Column(name = "procurement_rule", length = 1)
    private String procurementRule;
    @Column(name = "validity_start_date")
    private LocalDateTime validityStartDate;
    @Column(name = "low_level_code", length = 3)
    private String lowLevelCode;
    @Column(name = "prod_no_in_gen_prod_in_prepack", length = 40)
    private String prodNoInGenProdInPrepack;
    @Column(name = "serial_identifier_assgmt_profile", length = 4)
    private String serialIdentifierAssgmtProfile;
    @Column(name = "industry_standard_name", length = 18)
    private String industryStandardName;
    @Column(name = "product_standard_id", length = 18)
    private String productStandardId;
    @Column(name = "international_article_number_cat", length = 2)
    private String internationalArticleNumberCat;
    @Column(name = "product_is_configurable")
    private Boolean productIsConfigurable;
    @Column(name = "external_product_group", length = 18)
    private String externalProductGroup;
    @Column(name = "cross_plant_configurable_product", length = 40)
    private String crossPlantConfigurableProduct;
    @Column(name = "serial_no_explicitness_level", length = 1)
    private String serialNoExplicitnessLevel;
    @Column(name = "manufacturer_part_profile", length = 4)
    private String manufacturerPartProfile;
    @Column(name = "qlty_mgmt_in_procmt_is_active")
    private Boolean qltyMgmtInProcmtIsActive;
    @Column(name = "industry_sector", length = 1)
    private String industrySector;
    @Column(name = "change_number", length = 12)
    private String changeNumber;
    @Column(name = "handling_indicator", length = 4)
    private String handlingIndicator;
    @Column(name = "warehouse_product_group", length = 4)
    private String warehouseProductGroup;
    @Column(name = "warehouse_storage_condition", length = 2)
    private String warehouseStorageCondition;
    @Column(name = "standard_handling_unit_type", length = 4)
    private String standardHandlingUnitType;
    @Column(name = "adjustment_profile", length = 3)
    private String adjustmentProfile;
    @Column(name = "preferred_unit_of_measure", length = 3)
    private String preferredUnitOfMeasure;
    @Column(name = "is_pilferable")
    private Boolean isPilferable;
    @Column(name = "is_relevant_for_hzds_substances")
    private Boolean isRelevantForHzdsSubstances;
    @Column(name = "quarantine_period", precision = 3, scale = 0)
    private BigDecimal quarantinePeriod;
    @Column(name = "time_unit_for_quarantine_period", length = 3)
    private String timeUnitForQuarantinePeriod;
    @Column(name = "quality_inspection_group", length = 4)
    private String qualityInspectionGroup;
    @Column(name = "authorization_group", length = 4)
    private String authorizationGroup;
    @Column(name = "document_is_created_by_cad")
    private Boolean documentIsCreatedByCad;
    @Column(name = "handling_unit_type", length = 4)
    private String handlingUnitType;
    @Column(name = "has_variable_tare_weight")
    private Boolean hasVariableTareWeight;
    @Column(name = "maximum_packaging_length", precision = 15, scale = 3)
    private BigDecimal maximumPackagingLength;
    @Column(name = "maximum_packaging_width", precision = 15, scale = 3)
    private BigDecimal maximumPackagingWidth;
    @Column(name = "maximum_packaging_height", precision = 15, scale = 3)
    private BigDecimal maximumPackagingHeight;
    @Column(name = "unit_for_max_packaging_dimensions", length = 3)
    private String unitForMaxPackagingDimensions;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getTraceable() { return traceable; }
    public void setTraceable(Boolean traceable) { this.traceable = traceable; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getProductOldId() { return productOldId; }
    public void setProductOldId(String productOldId) { this.productOldId = productOldId; }
    public String getProductGroup() { return productGroup; }
    public void setProductGroup(String productGroup) { this.productGroup = productGroup; }
    public BigDecimal getGrossWeight() { return grossWeight; }
    public void setGrossWeight(BigDecimal grossWeight) { this.grossWeight = grossWeight; }
    public BigDecimal getNetWeight() { return netWeight; }
    public void setNetWeight(BigDecimal netWeight) { this.netWeight = netWeight; }
    public String getWeightUnit() { return weightUnit; }
    public void setWeightUnit(String weightUnit) { this.weightUnit = weightUnit; }
    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }
    public String getProductHierarchy() { return productHierarchy; }
    public void setProductHierarchy(String productHierarchy) { this.productHierarchy = productHierarchy; }
    public String getDivisionCode() { return divisionCode; }
    public void setDivisionCode(String divisionCode) { this.divisionCode = divisionCode; }
    public String getManufacturerNumber() { return manufacturerNumber; }
    public void setManufacturerNumber(String manufacturerNumber) { this.manufacturerNumber = manufacturerNumber; }
    public String getManufacturerPartNumber() { return manufacturerPartNumber; }
    public void setManufacturerPartNumber(String manufacturerPartNumber) { this.manufacturerPartNumber = manufacturerPartNumber; }
    public String getMaterialRevisionLevel() { return materialRevisionLevel; }
    public void setMaterialRevisionLevel(String materialRevisionLevel) { this.materialRevisionLevel = materialRevisionLevel; }
    public String getSerialNumberProfile() { return serialNumberProfile; }
    public void setSerialNumberProfile(String serialNumberProfile) { this.serialNumberProfile = serialNumberProfile; }
    public Boolean getBatchManaged() { return batchManaged; }
    public void setBatchManaged(Boolean batchManaged) { this.batchManaged = batchManaged; }
    public Boolean getMarkedForDeletion() { return markedForDeletion; }
    public void setMarkedForDeletion(Boolean markedForDeletion) { this.markedForDeletion = markedForDeletion; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getCustomerPartNumber() { return customerPartNumber; }
    public void setCustomerPartNumber(String customerPartNumber) { this.customerPartNumber = customerPartNumber; }
    public String getProductModel() { return productModel; }
    public void setProductModel(String productModel) { this.productModel = productModel; }
    public String getDrawingNumber() { return drawingNumber; }
    public void setDrawingNumber(String drawingNumber) { this.drawingNumber = drawingNumber; }
    public LocalDateTime getSapCreatedAt() { return sapCreatedAt; }
    public void setSapCreatedAt(LocalDateTime sapCreatedAt) { this.sapCreatedAt = sapCreatedAt; }
    public LocalDateTime getSapChangedAt() { return sapChangedAt; }
    public void setSapChangedAt(LocalDateTime sapChangedAt) { this.sapChangedAt = sapChangedAt; }
    public LocalDateTime getSapLastSyncAt() { return sapLastSyncAt; }
    public void setSapLastSyncAt(LocalDateTime sapLastSyncAt) { this.sapLastSyncAt = sapLastSyncAt; }
    public String getSapPayload() { return sapPayload; }
    public void setSapPayload(String sapPayload) { this.sapPayload = sapPayload; }
    public BigDecimal getMinPackagingQty() { return minPackagingQty; }
    public void setMinPackagingQty(BigDecimal minPackagingQty) { this.minPackagingQty = minPackagingQty; }
    public String getProcurementType() { return procurementType; }
    public void setProcurementType(String procurementType) { this.procurementType = procurementType; }
    public BigDecimal getSafetyStockQuantity() { return safetyStockQuantity; }
    public void setSafetyStockQuantity(BigDecimal safetyStockQuantity) { this.safetyStockQuantity = safetyStockQuantity; }
    public BigDecimal getLotSizeRoundingQuantity() { return lotSizeRoundingQuantity; }
    public void setLotSizeRoundingQuantity(BigDecimal lotSizeRoundingQuantity) { this.lotSizeRoundingQuantity = lotSizeRoundingQuantity; }
    public BigDecimal getOverDeliveryTolerance() { return overDeliveryTolerance; }
    public void setOverDeliveryTolerance(BigDecimal overDeliveryTolerance) { this.overDeliveryTolerance = overDeliveryTolerance; }
    public Boolean getUnlimitedOverDelivery() { return unlimitedOverDelivery; }
    public void setUnlimitedOverDelivery(Boolean unlimitedOverDelivery) { this.unlimitedOverDelivery = unlimitedOverDelivery; }
    public String getProductionStorageLocation() { return productionStorageLocation; }
    public void setProductionStorageLocation(String productionStorageLocation) { this.productionStorageLocation = productionStorageLocation; }
    public String getDefaultStorageLocation() { return defaultStorageLocation; }
    public void setDefaultStorageLocation(String defaultStorageLocation) { this.defaultStorageLocation = defaultStorageLocation; }
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    public String getYy1ColorNumber() { return yy1ColorNumber; }
    public void setYy1ColorNumber(String yy1ColorNumber) { this.yy1ColorNumber = yy1ColorNumber; }
    public String getYy1FifoProsign() { return yy1FifoProsign; }
    public void setYy1FifoProsign(String yy1FifoProsign) { this.yy1FifoProsign = yy1FifoProsign; }
    public String getYy1MoistureLevel() { return yy1MoistureLevel; }
    public void setYy1MoistureLevel(String yy1MoistureLevel) { this.yy1MoistureLevel = yy1MoistureLevel; }
    public String getYy1MoistureSensitive() { return yy1MoistureSensitive; }
    public void setYy1MoistureSensitive(String yy1MoistureSensitive) { this.yy1MoistureSensitive = yy1MoistureSensitive; }
    public String getYy1ShapeAndSize() { return yy1ShapeAndSize; }
    public void setYy1ShapeAndSize(String yy1ShapeAndSize) { this.yy1ShapeAndSize = yy1ShapeAndSize; }
    public String getYy1Material() { return yy1Material; }
    public void setYy1Material(String yy1Material) { this.yy1Material = yy1Material; }
    public String getYy1BrandM() { return yy1BrandM; }
    public void setYy1BrandM(String yy1BrandM) { this.yy1BrandM = yy1BrandM; }
    public String getYy1Designer() { return yy1Designer; }
    public void setYy1Designer(String yy1Designer) { this.yy1Designer = yy1Designer; }
    public String getYy1Cavity() { return yy1Cavity; }
    public void setYy1Cavity(String yy1Cavity) { this.yy1Cavity = yy1Cavity; }
    public String getYy1ColorRegion() { return yy1ColorRegion; }
    public void setYy1ColorRegion(String yy1ColorRegion) { this.yy1ColorRegion = yy1ColorRegion; }
    public String getYy1PlmPackageNumber() { return yy1PlmPackageNumber; }
    public void setYy1PlmPackageNumber(String yy1PlmPackageNumber) { this.yy1PlmPackageNumber = yy1PlmPackageNumber; }
    public String getYy1ProductTypeCustom() { return yy1ProductTypeCustom; }
    public void setYy1ProductTypeCustom(String yy1ProductTypeCustom) { this.yy1ProductTypeCustom = yy1ProductTypeCustom; }
    public String getYy1ProcessTreatment() { return yy1ProcessTreatment; }
    public void setYy1ProcessTreatment(String yy1ProcessTreatment) { this.yy1ProcessTreatment = yy1ProcessTreatment; }
    public String getYy1DescriptionOtp() { return yy1DescriptionOtp; }
    public void setYy1DescriptionOtp(String yy1DescriptionOtp) { this.yy1DescriptionOtp = yy1DescriptionOtp; }
    public String getYy1Encapsulation() { return yy1Encapsulation; }
    public void setYy1Encapsulation(String yy1Encapsulation) { this.yy1Encapsulation = yy1Encapsulation; }
    public String getYy1Project() { return yy1Project; }
    public void setYy1Project(String yy1Project) { this.yy1Project = yy1Project; }
    public String getYy1ExteriorColor() { return yy1ExteriorColor; }
    public void setYy1ExteriorColor(String yy1ExteriorColor) { this.yy1ExteriorColor = yy1ExteriorColor; }
    public String getCrossPlantStatus() { return crossPlantStatus; }
    public void setCrossPlantStatus(String crossPlantStatus) { this.crossPlantStatus = crossPlantStatus; }
    public LocalDateTime getCrossPlantStatusValidityDate() { return crossPlantStatusValidityDate; }
    public void setCrossPlantStatusValidityDate(LocalDateTime crossPlantStatusValidityDate) { this.crossPlantStatusValidityDate = crossPlantStatusValidityDate; }
    public String getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(String createdByUser) { this.createdByUser = createdByUser; }
    public String getLastChangedByUser() { return lastChangedByUser; }
    public void setLastChangedByUser(String lastChangedByUser) { this.lastChangedByUser = lastChangedByUser; }
    public String getPurchaseOrderQuantityUnit() { return purchaseOrderQuantityUnit; }
    public void setPurchaseOrderQuantityUnit(String purchaseOrderQuantityUnit) { this.purchaseOrderQuantityUnit = purchaseOrderQuantityUnit; }
    public String getSourceOfSupply() { return sourceOfSupply; }
    public void setSourceOfSupply(String sourceOfSupply) { this.sourceOfSupply = sourceOfSupply; }
    public String getCompetitorId() { return competitorId; }
    public void setCompetitorId(String competitorId) { this.competitorId = competitorId; }
    public String getItemCategoryGroup() { return itemCategoryGroup; }
    public void setItemCategoryGroup(String itemCategoryGroup) { this.itemCategoryGroup = itemCategoryGroup; }
    public String getVarblPurOrdUnitIsActive() { return varblPurOrdUnitIsActive; }
    public void setVarblPurOrdUnitIsActive(String varblPurOrdUnitIsActive) { this.varblPurOrdUnitIsActive = varblPurOrdUnitIsActive; }
    public String getVolumeUnit() { return volumeUnit; }
    public void setVolumeUnit(String volumeUnit) { this.volumeUnit = volumeUnit; }
    public BigDecimal getMaterialVolume() { return materialVolume; }
    public void setMaterialVolume(BigDecimal materialVolume) { this.materialVolume = materialVolume; }
    public String getAnpCode() { return anpCode; }
    public void setAnpCode(String anpCode) { this.anpCode = anpCode; }
    public String getProcurementRule() { return procurementRule; }
    public void setProcurementRule(String procurementRule) { this.procurementRule = procurementRule; }
    public LocalDateTime getValidityStartDate() { return validityStartDate; }
    public void setValidityStartDate(LocalDateTime validityStartDate) { this.validityStartDate = validityStartDate; }
    public String getLowLevelCode() { return lowLevelCode; }
    public void setLowLevelCode(String lowLevelCode) { this.lowLevelCode = lowLevelCode; }
    public String getProdNoInGenProdInPrepack() { return prodNoInGenProdInPrepack; }
    public void setProdNoInGenProdInPrepack(String prodNoInGenProdInPrepack) { this.prodNoInGenProdInPrepack = prodNoInGenProdInPrepack; }
    public String getSerialIdentifierAssgmtProfile() { return serialIdentifierAssgmtProfile; }
    public void setSerialIdentifierAssgmtProfile(String serialIdentifierAssgmtProfile) { this.serialIdentifierAssgmtProfile = serialIdentifierAssgmtProfile; }
    public String getIndustryStandardName() { return industryStandardName; }
    public void setIndustryStandardName(String industryStandardName) { this.industryStandardName = industryStandardName; }
    public String getProductStandardId() { return productStandardId; }
    public void setProductStandardId(String productStandardId) { this.productStandardId = productStandardId; }
    public String getInternationalArticleNumberCat() { return internationalArticleNumberCat; }
    public void setInternationalArticleNumberCat(String internationalArticleNumberCat) { this.internationalArticleNumberCat = internationalArticleNumberCat; }
    public Boolean getProductIsConfigurable() { return productIsConfigurable; }
    public void setProductIsConfigurable(Boolean productIsConfigurable) { this.productIsConfigurable = productIsConfigurable; }
    public String getExternalProductGroup() { return externalProductGroup; }
    public void setExternalProductGroup(String externalProductGroup) { this.externalProductGroup = externalProductGroup; }
    public String getCrossPlantConfigurableProduct() { return crossPlantConfigurableProduct; }
    public void setCrossPlantConfigurableProduct(String crossPlantConfigurableProduct) { this.crossPlantConfigurableProduct = crossPlantConfigurableProduct; }
    public String getSerialNoExplicitnessLevel() { return serialNoExplicitnessLevel; }
    public void setSerialNoExplicitnessLevel(String serialNoExplicitnessLevel) { this.serialNoExplicitnessLevel = serialNoExplicitnessLevel; }
    public String getManufacturerPartProfile() { return manufacturerPartProfile; }
    public void setManufacturerPartProfile(String manufacturerPartProfile) { this.manufacturerPartProfile = manufacturerPartProfile; }
    public Boolean getQltyMgmtInProcmtIsActive() { return qltyMgmtInProcmtIsActive; }
    public void setQltyMgmtInProcmtIsActive(Boolean qltyMgmtInProcmtIsActive) { this.qltyMgmtInProcmtIsActive = qltyMgmtInProcmtIsActive; }
    public String getIndustrySector() { return industrySector; }
    public void setIndustrySector(String industrySector) { this.industrySector = industrySector; }
    public String getChangeNumber() { return changeNumber; }
    public void setChangeNumber(String changeNumber) { this.changeNumber = changeNumber; }
    public String getHandlingIndicator() { return handlingIndicator; }
    public void setHandlingIndicator(String handlingIndicator) { this.handlingIndicator = handlingIndicator; }
    public String getWarehouseProductGroup() { return warehouseProductGroup; }
    public void setWarehouseProductGroup(String warehouseProductGroup) { this.warehouseProductGroup = warehouseProductGroup; }
    public String getWarehouseStorageCondition() { return warehouseStorageCondition; }
    public void setWarehouseStorageCondition(String warehouseStorageCondition) { this.warehouseStorageCondition = warehouseStorageCondition; }
    public String getStandardHandlingUnitType() { return standardHandlingUnitType; }
    public void setStandardHandlingUnitType(String standardHandlingUnitType) { this.standardHandlingUnitType = standardHandlingUnitType; }
    public String getAdjustmentProfile() { return adjustmentProfile; }
    public void setAdjustmentProfile(String adjustmentProfile) { this.adjustmentProfile = adjustmentProfile; }
    public String getPreferredUnitOfMeasure() { return preferredUnitOfMeasure; }
    public void setPreferredUnitOfMeasure(String preferredUnitOfMeasure) { this.preferredUnitOfMeasure = preferredUnitOfMeasure; }
    public Boolean getIsPilferable() { return isPilferable; }
    public void setIsPilferable(Boolean isPilferable) { this.isPilferable = isPilferable; }
    public Boolean getIsRelevantForHzdsSubstances() { return isRelevantForHzdsSubstances; }
    public void setIsRelevantForHzdsSubstances(Boolean isRelevantForHzdsSubstances) { this.isRelevantForHzdsSubstances = isRelevantForHzdsSubstances; }
    public BigDecimal getQuarantinePeriod() { return quarantinePeriod; }
    public void setQuarantinePeriod(BigDecimal quarantinePeriod) { this.quarantinePeriod = quarantinePeriod; }
    public String getTimeUnitForQuarantinePeriod() { return timeUnitForQuarantinePeriod; }
    public void setTimeUnitForQuarantinePeriod(String timeUnitForQuarantinePeriod) { this.timeUnitForQuarantinePeriod = timeUnitForQuarantinePeriod; }
    public String getQualityInspectionGroup() { return qualityInspectionGroup; }
    public void setQualityInspectionGroup(String qualityInspectionGroup) { this.qualityInspectionGroup = qualityInspectionGroup; }
    public String getAuthorizationGroup() { return authorizationGroup; }
    public void setAuthorizationGroup(String authorizationGroup) { this.authorizationGroup = authorizationGroup; }
    public Boolean getDocumentIsCreatedByCad() { return documentIsCreatedByCad; }
    public void setDocumentIsCreatedByCad(Boolean documentIsCreatedByCad) { this.documentIsCreatedByCad = documentIsCreatedByCad; }
    public String getHandlingUnitType() { return handlingUnitType; }
    public void setHandlingUnitType(String handlingUnitType) { this.handlingUnitType = handlingUnitType; }
    public Boolean getHasVariableTareWeight() { return hasVariableTareWeight; }
    public void setHasVariableTareWeight(Boolean hasVariableTareWeight) { this.hasVariableTareWeight = hasVariableTareWeight; }
    public BigDecimal getMaximumPackagingLength() { return maximumPackagingLength; }
    public void setMaximumPackagingLength(BigDecimal maximumPackagingLength) { this.maximumPackagingLength = maximumPackagingLength; }
    public BigDecimal getMaximumPackagingWidth() { return maximumPackagingWidth; }
    public void setMaximumPackagingWidth(BigDecimal maximumPackagingWidth) { this.maximumPackagingWidth = maximumPackagingWidth; }
    public BigDecimal getMaximumPackagingHeight() { return maximumPackagingHeight; }
    public void setMaximumPackagingHeight(BigDecimal maximumPackagingHeight) { this.maximumPackagingHeight = maximumPackagingHeight; }
    public String getUnitForMaxPackagingDimensions() { return unitForMaxPackagingDimensions; }
    public void setUnitForMaxPackagingDimensions(String unitForMaxPackagingDimensions) { this.unitForMaxPackagingDimensions = unitForMaxPackagingDimensions; }
}
