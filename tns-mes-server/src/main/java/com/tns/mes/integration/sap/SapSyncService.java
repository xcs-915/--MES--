package com.tns.mes.integration.sap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.mes.engineering.domain.Product;
import com.tns.mes.engineering.domain.Bom;
import com.tns.mes.engineering.domain.BomItem;
import com.tns.mes.engineering.domain.ProcessOperation;
import com.tns.mes.engineering.domain.ProcessRoute;
import com.tns.mes.engineering.repo.BomRepository;
import com.tns.mes.engineering.repo.ProcessRouteRepository;
import com.tns.mes.engineering.repo.ProductRepository;
import com.tns.mes.integration.ExternalApiClient;
import com.tns.mes.integration.outbox.OutboxService;
import com.tns.mes.integration.sap.service.ApiCallLogService;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.domain.WorkOrderOperation;
import com.tns.mes.production.repo.WorkOrderRepository;
import com.tns.mes.quality.domain.Batch;
import com.tns.mes.quality.repo.BatchRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SapSyncService {
    private static final Logger log = LoggerFactory.getLogger(SapSyncService.class);
    private final ExternalApiClient client;
    private final SapProperties properties;
    private final ObjectMapper mapper;
    private final ProductRepository products;
    private final BomRepository boms;
    private final ProcessRouteRepository routes;
    private final WorkOrderRepository workOrders;
    private final BatchRepository batches;
    private final OutboxService outbox;
    private final ApiCallLogService apiCallLogs;

    public SapSyncService(ExternalApiClient client, SapProperties properties, ObjectMapper mapper,
                          ProductRepository products, BomRepository boms, ProcessRouteRepository routes,
                          WorkOrderRepository workOrders, BatchRepository batches, OutboxService outbox, ApiCallLogService apiCallLogs) {
        this.client = client; this.properties = properties; this.mapper = mapper;
        this.products = products; this.boms = boms; this.routes = routes; this.workOrders = workOrders; this.batches = batches; this.outbox = outbox;
        this.apiCallLogs = apiCallLogs;
    }

    @Transactional
    public SyncResult syncProduct(String code) {
        if (code == null || code.trim().isEmpty()) throw new BizException(4003, "error.validation");
        return syncProducts(null, Collections.<String, Object>singletonMap("$filter", "Product eq '" + code.trim().replace("'", "''") + "'"));
    }

    @Transactional
    public SyncResult syncWorkOrder(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) throw new BizException(4003, "error.validation");
        return syncWorkOrders(null, Collections.<String, Object>singletonMap("$filter", "ManufacturingOrder eq '" + orderNo.trim().replace("'", "''") + "'"));
    }

    @Transactional
    public SyncResult syncBatch(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) throw new BizException(4003, "error.validation");
        return syncBatches(null, Collections.<String, Object>singletonMap("$filter", "Batch eq '" + batchNo.trim().replace("'", "''") + "'"));
    }

    public ExternalApiClient.ExternalApiResponse request(HttpMethod method, String path, Map<String, ?> query, Object body) {
        requireEnabled();
        return client.execute(properties.getBaseUrl(), path, method, authHeaders(), query, body);
    }

    @Transactional
    public SyncResult syncProducts(String path, Map<String, ?> query) {
        Map<String, Object> productQuery = new HashMap<>();
        if (query != null) productQuery.putAll(query);
        productQuery.putIfAbsent("$filter", recentChangeFilter("LastChangeDateTime"));
        productQuery.putIfAbsent("$top", properties.getPageSize());
        productQuery.putIfAbsent("$expand", "to_Description,to_BasicText,to_Plant,to_ProductUnitsOfMeasure,to_SalesDelivery,to_Valuation");
        productQuery.putIfAbsent("$select",
                "Product,ProductOldID,ProductGroup,ProductType,BaseUnit,CrossPlantStatus,CrossPlantStatusValidityDate,"
                + "CreationDate,CreatedByUser,LastChangeDate,LastChangedByUser,LastChangeDateTime,"
                + "GrossWeight,NetWeight,WeightUnit,CountryOfOrigin,ProductHierarchy,Division,"
                + "ProductManufacturerNumber,ManufacturerNumber,MaterialRevisionLevel,SerialNumberProfile,SerialIdentifierAssgmtProfile,"
                + "IsBatchManagementRequired,IsMarkedForDeletion,IsPilferable,IsRelevantForHzdsSubstances,"
                + "PurchaseOrderQuantityUnit,SourceOfSupply,CompetitorID,ItemCategoryGroup,"
                + "VarblPurOrdUnitIsActive,VolumeUnit,MaterialVolume,ANPCode,Brand,ProcurementRule,"
                + "ValidityStartDate,LowLevelCode,ProdNoInGenProdInPrepackProd,SizeOrDimensionText,"
                + "IndustryStandardName,ProductStandardID,InternationalArticleNumberCat,ProductIsConfigurable,"
                + "ExternalProductGroup,CrossPlantConfigurableProduct,SerialNoExplicitnessLevel,"
                + "ManufacturerPartProfile,QltyMgmtInProcmtIsActive,IndustrySector,ChangeNumber,"
                + "HandlingIndicator,WarehouseProductGroup,WarehouseStorageCondition,StandardHandlingUnitType,"
                + "AdjustmentProfile,PreferredUnitOfMeasure,QuarantinePeriod,TimeUnitForQuarantinePeriod,"
                + "QualityInspectionGroup,AuthorizationGroup,DocumentIsCreatedByCAD,HandlingUnitType,"
                + "HasVariableTareWeight,MaximumPackagingLength,MaximumPackagingWidth,MaximumPackagingHeight,"
                + "UnitForMaxPackagingDimensions,"
                + "YY1_F_Specification_PRD,YY1_F_BrandM_PRD,YY1_F_Color_PRD,YY1_F_ExteriorColor_PRD,"
                + "YY1_F_CustomerPartN_PRD,YY1_F_ProductModel_PRD,YY1_F_DrawingNo_PRD,SizeOrDimensionText,"
                + "YY1_F_ColorNumber_PRD,YY1_F_FIFOProsign_PRD,YY1_F_Moisturlever_PRD,YY1_F_Moisturesensiti_PRD,"
                + "YY1_F_ShapeAndSize_PRD,YY1_F_Material_PRD,YY1_F_Designer_PRD,YY1_F_Cavity_PRD,"
                + "YY1_F_ColorRegion_PRD,YY1_PLM_Package_number_PRD,YY1_ProductType_PRD,"
                + "YY1_F_ProcessTreatmen_PRD,YY1_F_DescriptionOTP_PRD,YY1_F_Encapsulation_PRD,"
                + "YY1_F_Project_PRD");
        List<JsonNode> rows;
        try {
            rows = fetchAllPages(pathOrDefault(path, properties.getProductPath()), productQuery);
        } catch (Exception ex) {
            // Fallback: SAP 可能不支持 to_BasicText 导航属性，移除后重试
            productQuery.put("$expand", "to_Description,to_Plant,to_ProductUnitsOfMeasure,to_SalesDelivery,to_Valuation");
            rows = fetchAllPages(pathOrDefault(path, properties.getProductPath()), productQuery);
        }
        int created = 0, updated = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        for (JsonNode row : rows) {
            try {
                String code = text(row, "code", "Code", "material", "Material", "MATNR", "product", "Product");
                if (code == null || code.trim().isEmpty()) throw new IllegalArgumentException("Missing material/product code");
                code = code.trim();
                Product product = products.findByCode(code).orElse(null);
                boolean isNew = product == null;
                if (isNew) product = new Product();
                product.setCode(code);
                String expandedDescription = productDescription(row);
                Map<String, String> langDescs = productDescriptionsByLanguage(row);
                String descZh = langDescs.get("ZH");
                String descEn = langDescs.get("EN");
                String descAr = langDescs.get("AR");
                String anyDesc = langDescs.values().stream().findFirst().orElse(null);
                String bestDesc = first(descZh, descEn, anyDesc, expandedDescription);
                String nameEn = first(text(row, "nameEn", "NameEn", "description", "Description", "ProductDescription", "MaterialDescription", "materialDescription"), descEn, bestDesc);
                String nameZh = first(text(row, "nameZh", "NameZh", "DescriptionZh", "ProductDescriptionZh"), descZh, bestDesc);
                String nameAr = first(text(row, "nameAr", "NameAr", "DescriptionAr", "ProductDescriptionAr"), descAr);
                product.setNameEn(nameEn);
                product.setNameZh(first(nameZh, nameEn, code));
                product.setNameAr(nameAr);
                product.setProductType(first(text(row, "productType", "ProductType", "materialType", "MaterialType"), "FINISHED"));
                product.setUnit(first(text(row, "unit", "Unit", "baseUnit", "BaseUnit", "MEINS"), "PCS"));
                product.setSpecification(first(text(row, "YY1_F_Specification_PRD", "SizeOrDimensionText", "specification", "Specification"), text(row, "ProductGroup", "MaterialGroup")));
                product.setStatus(Boolean.TRUE.equals(boolObject(row, "IsMarkedForDeletion")) ? "INACTIVE" : normalizeStatus(text(row, "CrossPlantStatus", "status", "Status", "isActive", "IsActive")));
                product.setTraceable(bool(row, true, "traceable", "Traceable"));
                product.setSource("SAP");
                product.setProductOldId(text(row, "ProductOldID"));
                product.setProductGroup(text(row, "ProductGroup"));
                product.setGrossWeight(decimal(row, "GrossWeight"));
                product.setNetWeight(decimal(row, "NetWeight"));
                product.setWeightUnit(text(row, "WeightUnit"));
                product.setCountryOfOrigin(text(row, "CountryOfOrigin"));
                product.setProductHierarchy(text(row, "ProductHierarchy"));
                product.setDivisionCode(text(row, "Division"));
                product.setManufacturerNumber(text(row, "ManufacturerNumber"));
                product.setManufacturerPartNumber(text(row, "ProductManufacturerNumber"));
                product.setMaterialRevisionLevel(text(row, "MaterialRevisionLevel"));
                product.setSerialNumberProfile(text(row, "SerialNumberProfile"));
                product.setBatchManaged(bool(row, false, "IsBatchManagementRequired"));
                product.setMarkedForDeletion(bool(row, false, "IsMarkedForDeletion"));
                product.setBrand(first(text(row, "YY1_F_BrandM_PRD", "Brand")));
                product.setColor(first(text(row, "YY1_F_Color_PRD", "YY1_F_ExteriorColor_PRD")));
                product.setCustomerPartNumber(text(row, "YY1_F_CustomerPartN_PRD"));
                product.setProductModel(text(row, "YY1_F_ProductModel_PRD"));
                product.setDrawingNumber(text(row, "YY1_F_DrawingNo_PRD"));
                product.setMinPackagingQty(decimal(row, "YY1_F_MinPackagingQty_PRD"));
                // YY1 custom fields
                product.setYy1ColorNumber(text(row, "YY1_F_ColorNumber_PRD"));
                product.setYy1FifoProsign(text(row, "YY1_F_FIFOProsign_PRD"));
                product.setYy1MoistureLevel(text(row, "YY1_F_Moisturlever_PRD"));
                product.setYy1MoistureSensitive(text(row, "YY1_F_Moisturesensiti_PRD"));
                product.setYy1ShapeAndSize(text(row, "YY1_F_ShapeAndSize_PRD"));
                product.setYy1Material(text(row, "YY1_F_Material_PRD"));
                product.setYy1BrandM(text(row, "YY1_F_BrandM_PRD"));
                product.setYy1Designer(text(row, "YY1_F_Designer_PRD"));
                product.setYy1Cavity(text(row, "YY1_F_Cavity_PRD"));
                product.setYy1ColorRegion(text(row, "YY1_F_ColorRegion_PRD"));
                product.setYy1PlmPackageNumber(text(row, "YY1_PLM_Package_number_PRD"));
                product.setYy1ProductTypeCustom(text(row, "YY1_ProductType_PRD"));
                product.setYy1ProcessTreatment(text(row, "YY1_F_ProcessTreatmen_PRD"));
                product.setYy1DescriptionOtp(text(row, "YY1_F_DescriptionOTP_PRD"));
                product.setYy1Encapsulation(text(row, "YY1_F_Encapsulation_PRD"));
                product.setYy1Project(text(row, "YY1_F_Project_PRD"));
                product.setYy1ExteriorColor(text(row, "YY1_F_ExteriorColor_PRD"));
                // SAP standard fields
                product.setCrossPlantStatus(text(row, "CrossPlantStatus"));
                product.setCrossPlantStatusValidityDate(dateTime(row, "CrossPlantStatusValidityDate"));
                product.setCreatedByUser(text(row, "CreatedByUser"));
                product.setLastChangedByUser(text(row, "LastChangedByUser"));
                product.setPurchaseOrderQuantityUnit(text(row, "PurchaseOrderQuantityUnit"));
                product.setSourceOfSupply(text(row, "SourceOfSupply"));
                product.setCompetitorId(text(row, "CompetitorID"));
                product.setItemCategoryGroup(text(row, "ItemCategoryGroup"));
                product.setVarblPurOrdUnitIsActive(text(row, "VarblPurOrdUnitIsActive"));
                product.setVolumeUnit(text(row, "VolumeUnit"));
                product.setMaterialVolume(decimal(row, "MaterialVolume"));
                product.setAnpCode(text(row, "ANPCode"));
                product.setProcurementRule(text(row, "ProcurementRule"));
                product.setValidityStartDate(dateTime(row, "ValidityStartDate"));
                product.setLowLevelCode(text(row, "LowLevelCode"));
                product.setProdNoInGenProdInPrepack(text(row, "ProdNoInGenProdInPrepackProd"));
                product.setSerialIdentifierAssgmtProfile(text(row, "SerialIdentifierAssgmtProfile"));
                product.setIndustryStandardName(text(row, "IndustryStandardName"));
                product.setProductStandardId(text(row, "ProductStandardID"));
                product.setInternationalArticleNumberCat(text(row, "InternationalArticleNumberCat"));
                product.setProductIsConfigurable(bool(row, false, "ProductIsConfigurable"));
                product.setExternalProductGroup(text(row, "ExternalProductGroup"));
                product.setCrossPlantConfigurableProduct(text(row, "CrossPlantConfigurableProduct"));
                product.setSerialNoExplicitnessLevel(text(row, "SerialNoExplicitnessLevel"));
                product.setManufacturerPartProfile(text(row, "ManufacturerPartProfile"));
                product.setQltyMgmtInProcmtIsActive(bool(row, false, "QltyMgmtInProcmtIsActive"));
                product.setIndustrySector(text(row, "IndustrySector"));
                product.setChangeNumber(text(row, "ChangeNumber"));
                product.setHandlingIndicator(text(row, "HandlingIndicator"));
                product.setWarehouseProductGroup(text(row, "WarehouseProductGroup"));
                product.setWarehouseStorageCondition(text(row, "WarehouseStorageCondition"));
                product.setStandardHandlingUnitType(text(row, "StandardHandlingUnitType"));
                product.setAdjustmentProfile(text(row, "AdjustmentProfile"));
                product.setPreferredUnitOfMeasure(text(row, "PreferredUnitOfMeasure"));
                product.setIsPilferable(bool(row, false, "IsPilferable"));
                product.setIsRelevantForHzdsSubstances(bool(row, false, "IsRelevantForHzdsSubstances"));
                product.setQuarantinePeriod(decimal(row, "QuarantinePeriod"));
                product.setTimeUnitForQuarantinePeriod(text(row, "TimeUnitForQuarantinePeriod"));
                product.setQualityInspectionGroup(text(row, "QualityInspectionGroup"));
                product.setAuthorizationGroup(text(row, "AuthorizationGroup"));
                product.setDocumentIsCreatedByCad(bool(row, false, "DocumentIsCreatedByCAD"));
                product.setHandlingUnitType(text(row, "HandlingUnitType"));
                product.setHasVariableTareWeight(bool(row, false, "HasVariableTareWeight"));
                product.setMaximumPackagingLength(decimal(row, "MaximumPackagingLength"));
                product.setMaximumPackagingWidth(decimal(row, "MaximumPackagingWidth"));
                product.setMaximumPackagingHeight(decimal(row, "MaximumPackagingHeight"));
                product.setUnitForMaxPackagingDimensions(text(row, "UnitForMaxPackagingDimensions"));
                // Extract from to_Description and to_BasicText expand
                Map<String, String> descMap = productDescriptionsByLanguage(row);
                String descFromExpand = first(descMap.get("ZH"), descMap.get("EN"), descMap.values().stream().findFirst().orElse(null));
                product.setProductDescription(descFromExpand);
                // Extract from to_Plant expand
                JsonNode plantNode = firstChild(row, "to_Plant", "to_PlantData");
                if (plantNode != null) {
                    product.setProcurementType(text(plantNode, "ProcurementType", "procurementType", "Procurement_Type"));
                    product.setSafetyStockQuantity(decimal(plantNode, "SafetyStockQuantity", "safetyStockQuantity", "SafetyStockQty"));
                    product.setLotSizeRoundingQuantity(decimal(plantNode, "LotSizeRoundingQuantity", "lotSizeRoundingQuantity", "LotSizeRoundingQty"));
                    product.setOverDeliveryTolerance(decimal(plantNode, "OverDelivToleranceLimit", "overDeliveryTolerance", "OverDeliveryTolerance"));
                    product.setUnlimitedOverDelivery(bool(plantNode, false, "UnlimitedOverDelivIsAllowed", "unlimitedOverDelivery", "OverDeliveryIsUnlimited"));
                    product.setProductionStorageLocation(text(plantNode, "ProductionInvtryManagedLoc", "productionStorageLocation", "ProductionStorageLocation"));
                    product.setDefaultStorageLocation(text(plantNode, "DfltStorageLocationExtProcmt", "defaultStorageLocation", "DefaultStorageLocation"));
                }
                product.setSapCreatedAt(dateTime(row, "CreationDate"));
                product.setSapChangedAt(dateTime(row, "LastChangeDateTime", "LastChangeDate"));
                product.setSapLastSyncAt(LocalDateTime.now());
                product.setSapPayload(row.toString());
                Product saved = products.save(product);
                outbox.enqueue("PRODUCT", String.valueOf(saved.getId()), isNew ? "PRODUCT_SYNC_CREATED" : "PRODUCT_SYNC_UPDATED", mapOf("source", "SAP", "code", code));
                if (isNew) created++; else updated++;
            } catch (RuntimeException ex) { failed++; errors.add(ex.getMessage()); }
        }
        return new SyncResult("PRODUCT", rows.size(), created, updated, failed, errors);
    }

    @Transactional
    public SyncResult syncWorkOrders(String path, Map<String, ?> query) {
        Map<String, Object> woQuery = new HashMap<>();
        if (query != null) woQuery.putAll(query);
        woQuery.putIfAbsent("$format", "json");
        woQuery.putIfAbsent("$inlinecount", "allpages");
        woQuery.putIfAbsent("$expand", "to_ProductionOrderOperation,to_ProductionOrderComponent,to_ProductionOrderItem");
        // Build $filter: always include Plant eq 'TK10'; if no filter provided, use recent change filter
        String existingFilter = (String) woQuery.get("$filter");
        if (existingFilter == null || existingFilter.trim().isEmpty()) {
            woQuery.put("$filter", workOrderRecentFilter());
        } else {
            // Convert LastChangeDateTime filter from datetimeoffset format to string format (yyyyMMddHHmmss)
            String converted = convertWoLastChangeFilter(existingFilter);
            // Ensure Plant filter is included
            if (!converted.toLowerCase().contains("plant")) {
                woQuery.put("$filter", "(Plant eq 'TK10') and (" + converted + ")");
            } else {
                woQuery.put("$filter", converted);
            }
        }
        String woPath = pathOrDefault(path, properties.getWorkOrderPath());

        // Step 1: Fetch all orders (API_PRODUCTION_ORDER_2_SRV returns one row per order)
        List<JsonNode> allRows;
        try {
            allRows = fetchAllPages(woPath, withPageSize(woQuery));
        } catch (Exception ex) {
            // Fallback: try without $format
            woQuery.remove("$format");
            allRows = fetchAllPages(woPath, withPageSize(woQuery));
        }

        int created = 0, updated = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        // Step 2: Process each work order
        for (JsonNode row : allRows) {
            String orderNo = text(row, "ManufacturingOrder", "OrderNo", "orderNo", "orderNumber", "OrderNumber", "manufacturingOrder", "AUFNR");
            if (orderNo == null || orderNo.trim().isEmpty()) continue;
            orderNo = orderNo.trim();
            try {
                String productCode = text(row, "Product", "productCode", "ProductCode", "material", "Material", "MATNR", "product");
                if (productCode == null) throw new IllegalArgumentException("Missing product code for order " + orderNo);
                productCode = productCode.trim();

                // Find product from items (more accurate) or header
                String itemProduct = null;
                BigDecimal itemQty = null;
                List<JsonNode> items = childRows(row, "to_ProductionOrderItem");
                for (JsonNode it : items) {
                    String p = text(it, "Product", "Material", "productCode");
                    if (p != null && !p.trim().isEmpty()) { itemProduct = p.trim(); itemQty = decimal(it, "MfgOrderPlannedTotalQty", "quantity", "Quantity"); break; }
                }
                if (itemProduct != null) productCode = itemProduct;

                final String finalProductCode = productCode;
                Product product = products.findByCode(productCode).orElseGet(() -> {
                    Product value = new Product();
                    value.setCode(finalProductCode);
                    value.setNameZh(first(text(row, "ProductName"), finalProductCode));
                    value.setProductType("FINISHED");
                    value.setUnit(first(text(row, "ProductionUnit"), "PCS"));
                    value.setProductModel(text(row, "YY1_F_ProductModel_PRD"));
                    value.setCustomerPartNumber(text(row, "YY1_F_CustomerPartN_PRD"));
                    value.setSource("SAP");
                    return products.save(value);
                });

                WorkOrder order = workOrders.findByOrderNo(orderNo).orElse(null);
                boolean isNew = order == null;
                if (isNew) order = new WorkOrder();
                order.setOrderNo(orderNo); order.setProduct(product);
                order.setQuantity(itemQty != null ? itemQty : decimal(row, "MfgOrderPlannedTotalQty", "quantity", "Quantity", "orderQuantity", "OrderQuantity", "plannedQuantity", "TotalQuantity"));
                if (order.getQuantity() == null || order.getQuantity().compareTo(BigDecimal.ZERO) <= 0) order.setQuantity(BigDecimal.ZERO);
                order.setPriority(integer(row, 50, "priority", "Priority"));
                order.setPlannedStart(dateTime(row, "MfgOrderPlannedStartDate", "plannedStart", "PlannedStart", "startDate", "StartDate", "BasicStartDate"));
                order.setPlannedEnd(dateTime(row, "MfgOrderPlannedEndDate", "plannedEnd", "PlannedEnd", "endDate", "EndDate", "BasicEndDate"));
                order.setCompletedQuantity(firstDecimal(decimal(row, "MfgOrderConfirmedYieldQty"), BigDecimal.ZERO));
                order.setStatus(workOrderStatus(row));
                order.setSource("SAP");
                order.setRemark(first(text(row, "ManufacturingOrderText", "OrderLongText"), text(row, "remark", "Remark", "description", "Description")));
                order.setOrderCategory(text(row, "ManufacturingOrderCategory"));
                order.setOrderType(text(row, "ManufacturingOrderType"));
                order.setProductionPlant(text(row, "ProductionPlant"));
                order.setPlant(text(row, "Plant"));
                order.setStorageLocation(text(row, "StorageLocation"));
                order.setMrpArea(text(row, "MRPArea"));
                order.setMrpController(text(row, "MRPController"));
                order.setProductionSupervisor(text(row, "ProductionSupervisor"));
                order.setProductionVersion(text(row, "ProductionVersion"));
                order.setPlannedOrder(text(row, "PlannedOrder"));
                order.setSalesOrder(text(row, "SalesOrder"));
                order.setSalesOrderItem(text(row, "SalesOrderItem"));
                order.setCompanyCode(text(row, "CompanyCode"));
                order.setProfitCenter(text(row, "ProfitCenter"));
                order.setScheduledStart(dateTime(row, "MfgOrderScheduledStartDate"));
                order.setScheduledEnd(dateTime(row, "MfgOrderScheduledEndDate"));
                order.setActualReleaseDate(date(row, "MfgOrderActualReleaseDate"));
                order.setProductionUnit(text(row, "ProductionUnit"));
                order.setPlannedScrapQuantity(decimal(row, "MfgOrderPlannedScrapQty"));
                order.setConfirmedYieldQuantity(decimal(row, "MfgOrderConfirmedYieldQty"));
                order.setOrderLongText(text(row, "OrderLongText"));
                order.setLocked(bool(row, false, "OrderIsLocked"));
                order.setMarkedForDeletion(bool(row, false, "OrderIsMarkedForDeletion", "OrderIsDeleted"));
                order.setSapCreatedAt(dateTime(row, "MfgOrderCreationDate", "CreationDate", "CreatedOn"));
                order.setSapChangedAt(dateTime(row, "LastChangeDateTime", "LastChangeDate"));
                order.setSapLastSyncAt(LocalDateTime.now());
                order.setSapPayload(row.toString());
                // Planned order + SAP extended fields
                order.setPlannedOrderType(text(row, "PlannedOrderType"));
                order.setPlannedOrderProfile(text(row, "PlannedOrderProfile"));
                order.setMaterialName(text(row, "MaterialName"));
                order.setMrpPlant(text(row, "MRPPlant"));
                order.setMaterialProcurementCategory(text(row, "MaterialProcurementCategory"));
                order.setMaterialProcurementType(text(row, "MaterialProcurementType"));
                order.setPlannedScrapQtySap(decimal(row, "PlndOrderPlannedScrapQty", "PlannedScrapQuantity"));
                order.setGoodsReceiptQty(decimal(row, "GoodsReceiptQty"));
                order.setIssuedQuantity(decimal(row, "IssuedQuantity"));
                order.setPlannedOrderOpeningDate(dateTime(row, "PlannedOrderOpeningDate"));
                order.setProductionStartDate(dateTime(row, "ProductionStartDate"));
                order.setProductionEndDate(dateTime(row, "ProductionEndDate"));
                order.setCustomer(text(row, "Customer"));
                order.setWbsElementInternalId(text(row, "WBSElementInternalID"));
                order.setWbsElement(text(row, "WBSElement"));
                order.setWbsDescription(text(row, "WBSDescription"));
                order.setAccountAssignmentCategory(text(row, "AccountAssignmentCategory"));
                order.setPurchasingGroup(text(row, "PurchasingGroup"));
                order.setPurchasingOrganization(text(row, "PurchasingOrganization"));
                order.setFixedSupplier(text(row, "FixedSupplier"));
                order.setPurchasingDocument(text(row, "PurchasingDocument"));
                order.setPurchasingDocumentItem(text(row, "PurchasingDocumentItem"));
                order.setQuotaArrangement(text(row, "QuotaArrangement"));
                order.setQuotaArrangementItem(text(row, "QuotaArrangementItem"));
                order.setSupplierName(text(row, "SupplierName"));
                order.setPlannedOrderIsFirm(bool(row, false, "PlannedOrderIsFirm"));
                order.setPlannedOrderIsConvertible(bool(row, false, "PlannedOrderIsConvertible"));
                order.setPlannedOrderBomIsFixed(bool(row, false, "PlannedOrderBOMIsFixed"));
                order.setPlannedOrderCapacityIsDsptchd(bool(row, false, "PlannedOrderCapacityIsDsptchd"));
                order.setCapacityRequirement(text(row, "CapacityRequirement"));
                order.setCapacityRequirementOrigin(text(row, "CapacityRequirementOrigin"));
                order.setBillOfOperationsType(text(row, "BillOfOperationsType"));
                order.setBillOfOperationsGroup(text(row, "BillOfOperationsGroup"));
                order.setBillOfOperations(text(row, "BillOfOperations"));
                order.setLastScheduledDate(dateTime(row, "LastScheduledDate"));
                order.setSchedulingType(text(row, "SchedulingType"));
                WorkOrder saved = workOrders.save(order);

                // Sync operations from $expand to_ProductionOrderOperation
                List<JsonNode> opRows = childRows(row, "to_ProductionOrderOperation");
                syncOperationsFromExpand(opRows, saved, product);

                // Sync components from $expand to_ProductionOrderComponent
                List<JsonNode> compRows = childRows(row, "to_ProductionOrderComponent");
                syncComponentsFromExpand(compRows, saved, product);

                outbox.enqueue("WORK_ORDER", String.valueOf(saved.getId()), isNew ? "WORK_ORDER_SYNC_CREATED" : "WORK_ORDER_SYNC_UPDATED", mapOf("source", "SAP", "orderNo", orderNo));
                if (isNew) created++; else updated++;
            } catch (RuntimeException ex) {
                failed++;
                String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                errors.add(orderNo + ": " + message);
                log.warn("SAP work order sync failed for {}", orderNo, ex);
            }
        }
        return new SyncResult("WORK_ORDER", allRows.size(), created, updated, failed, errors);
    }

    @Transactional
    public SyncResult syncBatches(String path, Map<String, ?> query) {
        Map<String, Object> batchQuery = new HashMap<>();
        if (query != null) batchQuery.putAll(query);
        batchQuery.putIfAbsent("$filter", recentChangeFilter("LastChangeDateTime"));
        batchQuery.putIfAbsent("$top", properties.getPageSize());
        batchQuery.putIfAbsent("$expand", "to_BatchPlant,to_BatchCharc");
        List<JsonNode> rows = fetchAllPages(pathOrDefault(path, properties.getBatchPath()), batchQuery);
        int created = 0, updated = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        for (JsonNode row : rows) {
            try {
                String batchNo = text(row, "Batch", "batchNo", "BatchNo", "CHARG", "batch");
                if (batchNo == null || batchNo.trim().isEmpty()) throw new IllegalArgumentException("Missing batch number");
                batchNo = batchNo.trim();
                String plant = first(text(row, "BatchIdentifyingPlant", "Plant", "plant", "WERKS"),
                        plantFromBatchExpand(row));
                Batch batch = null;
                if (plant != null && !plant.trim().isEmpty()) {
                    batch = batches.findByBatchNoAndPlant(batchNo, plant.trim()).orElse(null);
                }
                if (batch == null) {
                    batch = batches.findByBatchNo(batchNo).orElse(null);
                }
                boolean isNew = batch == null;
                if (isNew) batch = new Batch();
                batch.setBatchNo(batchNo);
                batch.setPlant(plant);
                batch.setProductCode(text(row, "Material", "Product", "productCode", "ProductCode", "MATNR", "material"));
                batch.setProductName(first(text(row, "MaterialName", "ProductName", "productName", "MaterialDescription"), batch.getProductCode()));
                batch.setBatchStatus(first(text(row, "BatchStatus", "batchStatus", "Status", "status", "ZUSTD"), "RELEASED"));
                batch.setAvailabilityDate(date(row, "AvailabilityDate", "availabilityDate", "AvailableDate"));
                batch.setExpirationDate(date(row, "ExpirationDate", "expirationDate", "ExpiryDate"));
                batch.setShelfLifeExpirationDate(date(row, "ShelfLifeExpirationDate", "shelfLifeExpirationDate", "SLED_BBD"));
                batch.setManufactureDate(date(row, "ManufactureDate", "manufactureDate", "ProductionDate", "MfgDate"));
                batch.setSupplierBatch(text(row, "BatchBySupplier", "SupplierBatch", "supplierBatch", "VendorBatch", "LICHN"));
                batch.setVendor(text(row, "Supplier", "Vendor", "vendor", "supplier", "LIFNR"));
                batch.setQuantity(decimal(row, "Quantity", "quantity", "BatchQuantity", "AvailableQty"));
                batch.setUnit(first(text(row, "Unit", "unit", "BaseUnit", "MEINS"), "PCS"));
                batch.setRestrictedUse(bool(row, false, "MatlBatchIsInRstrcdUseStock", "RestrictedUse", "restrictedUse", "IsRestricted", "INSMK"));
                batch.setInspectionLot(text(row, "InspectionLot", "inspectionLot", "InspectionLotNumber", "PRUEFLOS"));
                batch.setInspectionStatus(text(row, "InspectionStatus", "inspectionStatus", "InspStatus"));
                batch.setBatchClass(text(row, "BatchClass", "batchClass", "ClassType", "KLART"));
                batch.setRemark(first(text(row, "BatchRemark", "remark", "Remark", "Description"), text(row, "BatchText", "BatchLongText")));
                batch.setBatchMarkedForDeletion(bool(row, false, "BatchIsMarkedForDeletion"));
                batch.setCountryOfOrigin(text(row, "CountryOfOrigin"));
                batch.setRegionOfOrigin(text(row, "RegionOfOrigin"));
                batch.setFreeDefinedDate1(date(row, "FreeDefinedDate1"));
                batch.setFreeDefinedDate2(date(row, "FreeDefinedDate2"));
                batch.setFreeDefinedDate3(date(row, "FreeDefinedDate3"));
                batch.setFreeDefinedDate4(date(row, "FreeDefinedDate4"));
                batch.setFreeDefinedDate5(date(row, "FreeDefinedDate5"));
                batch.setFreeDefinedDate6(date(row, "FreeDefinedDate6"));
                batch.setNextInspectionDate(date(row, "NextInspectionDate"));
                batch.setLastGoodsReceiptDate(date(row, "LastGoodsReceiptDate"));
                batch.setExportImportProductGroup(text(row, "ExportAndImportProductGroup"));
                batch.setBatchCertificationDate(date(row, "BatchCertificationDate"));
                batch.setMaterial(text(row, "Material"));
                batch.setBatchIdentifyingPlant(text(row, "BatchIdentifyingPlant"));
                batch.setSource("SAP");
                batch.setSapCreatedAt(dateTime(row, "CreationDateTime", "CreationDate", "CreatedOn"));
                batch.setSapChangedAt(dateTime(row, "LastChangeDateTime", "LastChangeDate", "ChangedOn"));
                batch.setSapLastSyncAt(LocalDateTime.now());
                batch.setSapPayload(row.toString());
                Batch saved = batches.save(batch);
                outbox.enqueue("BATCH", String.valueOf(saved.getId()), isNew ? "BATCH_SYNC_CREATED" : "BATCH_SYNC_UPDATED", mapOf("source", "SAP", "batchNo", batchNo));
                if (isNew) created++; else updated++;
            } catch (RuntimeException ex) { failed++; errors.add(ex.getMessage()); }
        }
        return new SyncResult("BATCH", rows.size(), created, updated, failed, errors);
    }

    /** Sync operations from $expand to_ProductionOrderOperation (API_PRODUCTION_ORDER_2_SRV). */
    private void syncOperationsFromExpand(List<JsonNode> opRows, WorkOrder order, Product product) {
        if (opRows == null || opRows.isEmpty()) return;
        String routeCode = "SAP-" + order.getOrderNo();
        ProcessRoute route = routes.findByProductIdAndCodeAndVersionCode(product.getId(), routeCode, "SAP").orElseGet(ProcessRoute::new);
        route.setProductId(product.getId());
        route.setCode(routeCode);
        route.setVersionCode("SAP");
        route.setNameZh("SAP工艺路线 " + order.getOrderNo());
        route.setNameEn("SAP Route " + order.getOrderNo());
        route.setNameAr("مسار SAP " + order.getOrderNo());
        route.setStatus("PUBLISHED");
        route.getOperations().clear();
        routes.saveAndFlush(route); // Force DELETE before INSERT to avoid unique constraint violation

        // Sort by operation number
        opRows.sort((a, b) -> {
            String sa = text(a, "ManufacturingOrderOperation", "Operation");
            String sb = text(b, "ManufacturingOrderOperation", "Operation");
            if (sa == null) sa = "";
            if (sb == null) sb = "";
            return sa.compareTo(sb);
        });

        int seq = 10;
        // Also sync WorkOrderOperation entries directly on the WorkOrder
        order.getOperations().clear();
        workOrders.saveAndFlush(order);
        for (JsonNode row : opRows) {
            String opCode = text(row, "ManufacturingOrderOperation", "Operation", "operationCode", "OperationCode", "OperationNumber");
            if (opCode == null || opCode.trim().isEmpty()) continue;
            String opName = first(text(row, "MfgOrderOperationText", "OperationText", "OperationDescription", "operationName", "OperationName"), opCode);

            // Save to ProcessRoute
            ProcessOperation item = new ProcessOperation();
            item.setSequenceNo(integer(row, seq, "sequenceNo", "SequenceNo", "OperationNumber", "ManufacturingOrderOperation"));
            item.setCode(opCode.trim());
            item.setNameZh(opName);
            item.setNameEn(first(text(row, "nameEn", "NameEn"), opName));
            item.setNameAr(first(text(row, "nameAr", "NameAr"), opName));
            item.setStandardTimeSeconds(integer(row, 0, "WorkCenterStandardWorkQty1", "standardTimeSeconds", "StandardTimeSeconds", "StandardDuration"));
            item.setPlant(text(row, "Plant"));
            item.setWorkCenterCode(text(row, "WorkCenter"));
            item.setWorkCenterInternalId(text(row, "WorkCenterInternalID", "WorkCenterInternalId"));
            item.setControlProfile(text(row, "OperationControlProfile"));
            item.setOperationUnit(text(row, "OperationUnit", "ProductionUnit"));
            item.setPlannedTotalQuantity(decimal(row, "OpPlannedTotalQuantity"));
            item.setPlannedYieldQuantity(decimal(row, "OpPlannedYieldQuantity"));
            item.setConfirmedYieldQuantity(decimal(row, "MfgOrderConfirmedYieldQty", "ConfirmedYieldQuantity"));
            route.addOperation(item);

            // Also create WorkOrderOperation entry
            WorkOrderOperation woo = new WorkOrderOperation();
            woo.setWorkOrder(order);
            woo.setSequenceNo(seq);
            woo.setStatus("PENDING");
            woo.setPlannedQuantity(firstDecimal(
                    decimal(row, "OpPlannedTotalQuantity", "OpPlannedYieldQuantity"),
                    firstDecimal(order.getQuantity(), BigDecimal.ZERO)));
            woo.setCompletedQuantity(firstDecimal(decimal(row, "MfgOrderConfirmedYieldQty"), BigDecimal.ZERO));
            woo.setOperationCode(opCode.trim());
            woo.setOperationName(opName);
            woo.setWorkCenterCode(text(row, "WorkCenter"));
            woo.setWorkCenterDesc(text(row, "WorkCenterText", "WorkCenterDescription"));
            woo.setPlant(text(row, "Plant"));
            woo.setControlKey(text(row, "OperationControlProfile"));
            woo.setOperationUnit(text(row, "OperationUnit", "ProductionUnit"));
            woo.setPlannedYieldQuantity(decimal(row, "OpPlannedYieldQuantity"));
            woo.setConfirmedYieldQuantity(decimal(row, "MfgOrderConfirmedYieldQty"));
            woo.setPlannedTotalQuantity(decimal(row, "OpPlannedTotalQuantity"));
            woo.setWorkCenterInternalId(text(row, "WorkCenterInternalID", "WorkCenterInternalId"));
            woo.setStandardTimeSeconds(integer(row, 0, "WorkCenterStandardWorkQty1", "standardTimeSeconds", "StandardTimeSeconds", "StandardDuration"));
            woo.setSapPayload(row.toString());
            order.addOperation(woo);

            seq += 10;
        }
        if (!route.getOperations().isEmpty()) { routes.save(route); order.setRoute(route); }
        if (!order.getOperations().isEmpty()) workOrders.save(order);
    }

    /** Sync components from $expand to_ProductionOrderComponent (API_PRODUCTION_ORDER_2_SRV). */
    private void syncComponentsFromExpand(List<JsonNode> compRows, WorkOrder order, Product product) {
        if (compRows == null || compRows.isEmpty()) return;

        String bomCode = "SAP-" + order.getOrderNo();
        Bom bom = boms.findByProductIdAndCodeAndVersionCode(product.getId(), bomCode, "SAP").orElseGet(Bom::new);
        bom.setProductId(product.getId()); bom.setCode(bomCode); bom.setVersionCode("SAP");
        bom.setNameZh("SAP BOM " + order.getOrderNo()); bom.setNameEn("SAP BOM " + order.getOrderNo());
        bom.setNameAr("قائمة SAP " + order.getOrderNo()); bom.setStatus("PUBLISHED");
        bom.getItems().clear();
        boms.saveAndFlush(bom); // Force DELETE before INSERT to avoid unique constraint violation

        int sequence = 10;
        for (JsonNode component : compRows) {
            String componentCode = text(component, "Material", "material", "Product", "Component", "MaterialComponent", "componentProductCode", "ComponentProductCode");
            BigDecimal quantity = decimal(component, "RequiredQuantity", "MfgOrderComponentQuantity", "quantity", "Quantity", "ComponentQuantity");
            if (componentCode == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) continue;
            componentCode = componentCode.trim();
            Product componentProduct = upsertComponentProduct(componentCode, component);
            BomItem item = new BomItem();
            item.setComponentProductId(componentProduct.getId());
            item.setSequenceNo(integer(component, sequence, "sequenceNo", "SequenceNo", "BillOfMaterialItemNumber", "BOMItem", "ReservationItem"));
            item.setQuantity(quantity);
            item.setUnit(first(text(component, "BaseUnit", "unit", "Unit"), "PCS"));
            item.setReservationNo(text(component, "Reservation"));
            item.setReservationItem(text(component, "ReservationItem"));
            item.setOperationCode(text(component, "ManufacturingOrderOperation"));
            item.setMaterialGroup(text(component, "MaterialGroup"));
            item.setRequirementDate(date(component, "MatlCompRequirementDate", "RequirementDate"));
            item.setWithdrawnQuantity(decimal(component, "WithdrawnQuantity"));
            item.setAvailableQuantity(decimal(component, "ConfirmedAvailableQuantity", "AvailableQuantity"));
            item.setStorageLocation(text(component, "StorageLocation"));
            item.setGoodsMovementType(text(component, "GoodsMovementType"));
            item.setBatch(text(component, "Batch"));
            item.setBackflush(bool(component, false, "MatlCompIsMarkedForBackflush", "Backflush", "IsBackflush"));
            item.setBomItemNumber(text(component, "BillOfMaterialItemNumber", "BOMItem"));
            item.setItemDescription(text(component, "BOMItemDescription", "OrderComponentLongText", "Description"));
            item.setDeleted(bool(component, false, "IsDeleted", "OrderItemIsDeleted"));
            item.setComponentPlant(text(component, "Plant"));
            item.setManufacturingOrder(text(component, "ManufacturingOrder"));
            item.setManufacturingOrderSequence(text(component, "ManufacturingOrderSequence"));
            item.setManufacturingOrderOperation(text(component, "ManufacturingOrderOperation"));
            item.setProductionPlant(text(component, "ProductionPlant"));
            item.setOrderInternalBillOfOperations(text(component, "OrderInternalBillOfOperations"));
            item.setRequirementTime(text(component, "MatlCompRequirementTime"));
            item.setReservationFinallyIssued(bool(component, false, "ReservationIsFinallyIssued"));
            item.setCostRelevant(text(component, "MaterialCompIsCostRelevant"));
            item.setSalesOrder(text(component, "SalesOrder"));
            item.setSalesOrderItem(text(component, "SalesOrderItem"));
            item.setSortField(text(component, "SortField"));
            item.setBomItemCategory(text(component, "BOMItemCategory"));
            item.setSupplyArea(text(component, "SupplyArea"));
            item.setGoodsRecipientName(text(component, "GoodsRecipientName"));
            item.setUnloadingPointName(text(component, "UnloadingPointName"));
            item.setAlternativeItem(bool(component, false, "MaterialCompIsAlternativeItem"));
            item.setAlternativeItemGroup(text(component, "AlternativeItemGroup"));
            item.setAlternativeItemStrategy(text(component, "AlternativeItemStrategy"));
            item.setAlternativeItemPriority(text(component, "AlternativeItemPriority"));
            item.setUsageProbabilityPercent(decimal(component, "UsageProbabilityPercent"));
            item.setPhantomItem(bool(component, false, "MaterialComponentIsPhantomItem"));
            item.setLeadTimeOffset(decimal(component, "LeadTimeOffset"));
            item.setQuantityIsFixed(bool(component, false, "QuantityIsFixed"));
            item.setNetScrap(bool(component, false, "IsNetScrap"));
            item.setComponentScrapInPercent(decimal(component, "ComponentScrapInPercent"));
            item.setOperationScrapInPercent(decimal(component, "OperationScrapInPercent"));
            item.setOriginalQuantity(decimal(component, "MaterialCompOriginalQuantity"));
            item.setEntryUnit(text(component, "EntryUnit"));
            item.setGoodsMovementEntryQty(decimal(component, "GoodsMovementEntryQty"));
            item.setBatchSplitType(text(component, "BatchSplitType"));
            item.setBaseUnitIsoCode(text(component, "BaseUnitISOCode"));
            item.setBaseUnitSapCode(text(component, "BaseUnitSAPCode"));
            item.setEntryUnitIsoCode(text(component, "EntryUnitISOCode"));
            item.setEntryUnitSapCode(text(component, "EntryUnitSAPCode"));
            item.setCurrency(text(component, "Currency"));
            item.setWithdrawnQuantityAmount(decimal(component, "WithdrawnQuantityAmount"));
            item.setSapPayload(component.toString());
            bom.addItem(item);
            sequence += 10;
        }
        if (!bom.getItems().isEmpty()) { boms.save(bom); order.setBom(bom); workOrders.save(order); }
    }

    /** Extract operations directly from CDS view rows (one operation per row). */
    private void syncOperationsFromRows(List<JsonNode> opRows, WorkOrder order, Product product) {
        if (opRows == null || opRows.isEmpty()) return;
        String routeCode = "SAP-" + order.getOrderNo();
        ProcessRoute route = routes.findByProductIdAndCodeAndVersionCode(product.getId(), routeCode, "SAP").orElseGet(ProcessRoute::new);
        route.setProductId(product.getId());
        route.setCode(routeCode);
        route.setVersionCode("SAP");
        route.setNameZh("SAP工艺路线 " + order.getOrderNo());
        route.setNameEn("SAP Route " + order.getOrderNo());
        route.setNameAr("مسار SAP " + order.getOrderNo());
        route.setStatus("PUBLISHED");
        route.getOperations().clear();
        routes.saveAndFlush(route); // Force DELETE before INSERT to avoid unique constraint violation

        int seq = 10;
        for (JsonNode row : opRows) {
            String opCode = text(row, "ManufacturingOrderOperation_2", "operationCode", "OperationCode", "operation", "Operation", "OperationNumber", "Activity");
            if (opCode == null || opCode.trim().isEmpty()) continue;
            String opName = first(text(row, "MfgOrderOperationText", "operationName", "OperationName", "OperationText", "OperationDescription"), opCode);
            ProcessOperation item = new ProcessOperation();
            item.setSequenceNo(integer(row, seq, "ManufacturingOrderOperation_2", "sequenceNo", "SequenceNo", "OperationNumber"));
            item.setCode(opCode.trim());
            item.setNameZh(opName);
            item.setNameEn(first(text(row, "nameEn", "NameEn"), opName));
            item.setNameAr(first(text(row, "nameAr", "NameAr"), opName));
            item.setStandardTimeSeconds(integer(row, 0, "WorkCenterStandardWorkQty1", "standardTimeSeconds", "StandardTimeSeconds", "StandardDuration"));
            item.setPlant(text(row, "Plant"));
            item.setWorkCenterCode(text(row, "WorkCenter"));
            item.setWorkCenterInternalId(text(row, "WorkCenterInternalID_1", "WorkCenterInternalID"));
            item.setControlProfile(text(row, "OperationControlProfile"));
            item.setOperationUnit(text(row, "OperationUnit", "ProductionUnit"));
            item.setPlannedTotalQuantity(decimal(row, "OpPlannedTotalQuantity"));
            item.setPlannedYieldQuantity(decimal(row, "OpPlannedYieldQuantity"));
            item.setConfirmedYieldQuantity(decimal(row, "MfgOrderConfirmedYieldQty"));
            route.addOperation(item);
            seq += 10;
        }
        if (!route.getOperations().isEmpty()) { routes.save(route); order.setRoute(route); workOrders.save(order); }
    }

    /** Try to fetch components via separate paths. */
    private void syncComponentsFallback(WorkOrder order, Product product) {
        List<JsonNode> componentRows = Collections.emptyList();
        String orderNo = order.getOrderNo().replace("'", "''");
        try {
            if (properties.getComponentPath() != null && !properties.getComponentPath().trim().isEmpty()) {
                componentRows = rows(getJson(properties.getComponentPath(), withPageSize(Collections.<String, Object>singletonMap("$filter", "ManufacturingOrder eq '" + orderNo + "'"))));
            }
        } catch (Exception ignored) {}
        try {
            if (componentRows.isEmpty()) {
                String componentPath = properties.getWorkOrderPath() + "('" + orderNo + "')/to_ProductionOrderComponent";
                componentRows = rows(getJson(componentPath, withPageSize(Collections.emptyMap())));
            }
        } catch (Exception ignored) {}
        if (componentRows.isEmpty()) return;

        String bomCode = "SAP-" + order.getOrderNo();
        Bom bom = boms.findByProductIdAndCodeAndVersionCode(product.getId(), bomCode, "SAP").orElseGet(Bom::new);
        bom.setProductId(product.getId()); bom.setCode(bomCode); bom.setVersionCode("SAP");
        bom.setNameZh("SAP BOM " + order.getOrderNo()); bom.setNameEn("SAP BOM " + order.getOrderNo());
        bom.setNameAr("قائمة SAP " + order.getOrderNo()); bom.setStatus("PUBLISHED");
        bom.getItems().clear();
        boms.saveAndFlush(bom); // Force DELETE before INSERT to avoid unique constraint violation

        int sequence = 10;
        for (JsonNode component : componentRows) {
            String componentCode = text(component, "componentProductCode", "ComponentProductCode", "material", "Material", "Product", "Component", "MaterialComponent");
            BigDecimal quantity = decimal(component, "quantity", "Quantity", "requiredQuantity", "RequiredQuantity", "ComponentQuantity", "MfgOrderComponentQuantity");
            if (componentCode == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) continue;
            Product componentProduct = upsertComponentProduct(componentCode, component);
            BomItem item = new BomItem();
            item.setComponentProductId(componentProduct.getId());
            item.setSequenceNo(integer(component, sequence, "sequenceNo", "SequenceNo", "BillOfMaterialItemNumber", "BOMItem"));
            item.setQuantity(quantity);
            item.setUnit(first(text(component, "unit", "Unit", "BaseUnit"), "PCS"));
            item.setReservationNo(text(component, "Reservation"));
            item.setReservationItem(text(component, "ReservationItem"));
            item.setOperationCode(text(component, "ManufacturingOrderOperation"));
            item.setMaterialGroup(text(component, "MaterialGroup"));
            item.setRequirementDate(date(component, "MatlCompRequirementDate"));
            item.setWithdrawnQuantity(decimal(component, "WithdrawnQuantity"));
            item.setAvailableQuantity(decimal(component, "ConfirmedAvailableQuantity"));
            item.setStorageLocation(text(component, "StorageLocation"));
            item.setGoodsMovementType(text(component, "GoodsMovementType"));
            item.setBomItemNumber(text(component, "BillOfMaterialItemNumber", "BOMItem"));
            item.setItemDescription(text(component, "BOMItemDescription", "OrderComponentLongText"));
            item.setBulkMaterial(bool(component, false, "IsBulkMaterialComponent"));
            item.setBackflush(bool(component, false, "MatlCompIsMarkedForBackflush"));
            item.setDeleted(bool(component, false, "MatlCompIsMarkedForDeletion"));
            item.setIssueMethod(Boolean.TRUE.equals(item.getBackflush()) ? "BACKFLUSH" : "MANUAL");
            item.setSapPayload(component.toString());
            bom.addItem(item); sequence += 10;
        }
        if (!bom.getItems().isEmpty()) { boms.save(bom); order.setBom(bom); workOrders.save(order); }
    }

    private Product upsertComponentProduct(String code, JsonNode row) {
        String value = code.trim();
        Product product = products.findByCode(value).orElse(null);
        boolean isNew = product == null;
        if (isNew) {
            product = new Product();
            product.setProductType("COMPONENT");
        }
        product.setCode(value);
        String name = first(text(row, "nameEn", "NameEn", "description", "Description", "ProductDescription"), value);
        product.setNameZh(first(text(row, "nameZh", "NameZh"), name));
        product.setNameEn(name);
        product.setNameAr(text(row, "nameAr", "NameAr"));
        product.setUnit(first(text(row, "unit", "Unit", "BaseUnit"), "PCS"));
        product.setStatus("ACTIVE");
        product.setTraceable(true);
        return products.save(product);
    }

    private List<JsonNode> childRows(JsonNode node, String... keys) { JsonNode value = node; for (String key : keys) { if (value != null && value.isObject() && value.has(key)) { value = value.get(key); break; } } if (value != null && value.isObject() && value.has("results")) value = value.get("results"); if (value != null && value.isObject() && value.has("value")) value = value.get("value"); List<JsonNode> rows = new ArrayList<>(); if (value != null && value.isArray()) value.forEach(rows::add); return rows; }

    /** Returns the first child object from an expand navigation property (handles both array and single object). */
    private JsonNode firstChild(JsonNode node, String... keys) {
        List<JsonNode> children = childRows(node, keys);
        return children.isEmpty() ? null : children.get(0);
    }

    private JsonNode getJson(String path, Map<String, ?> query) {
        long start = System.currentTimeMillis();
        Integer status = null;
        String body = null;
        String errorMsg = null;
        boolean success = false;
        try {
            ExternalApiClient.ExternalApiResponse response = request(HttpMethod.GET, path, query, null);
            status = response.getStatus();
            body = response.getBody();
            if (!response.is2xx()) {
                errorMsg = "SAP returned HTTP " + response.getStatus();
                throw new IllegalStateException(errorMsg);
            }
            try {
                JsonNode result = mapper.readTree(response.getBody() == null ? "{}" : response.getBody());
                success = true;
                return result;
            } catch (Exception ex) {
                errorMsg = "Invalid SAP JSON response: " + ex.getMessage();
                throw new IllegalStateException("Invalid SAP JSON response", ex);
            }
        } catch (RuntimeException ex) {
            if (errorMsg == null) errorMsg = ex.getMessage();
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            apiCallLogs.logCall("SAP", path, "GET", query, null, status, body, duration, success, errorMsg);
        }
    }

    /** Fetches all pages from SAP OData by following __next links. */
    private List<JsonNode> fetchAllPages(String path, Map<String, ?> query) {
        List<JsonNode> allRows = new ArrayList<>();
        JsonNode root = getJson(path, query);
        allRows.addAll(rows(root));
        int pageCount = 0;
        while (pageCount < 50) {
            String nextLink = nextLink(root);
            if (nextLink == null || nextLink.trim().isEmpty()) break;
            String nextPath = extractPath(nextLink, path);
            Map<String, Object> nextQuery = extractQuery(nextLink);
            root = getJson(nextPath, nextQuery);
            allRows.addAll(rows(root));
            pageCount++;
        }
        return allRows;
    }

    /** Extracts the __next link from an OData v2 response. */
    private String nextLink(JsonNode root) {
        if (root == null) return null;
        // OData v2: d.__next
        if (root.has("d") && root.get("d").isObject()) {
            JsonNode d = root.get("d");
            if (d.has("__next")) return d.get("__next").asText();
        }
        // OData v4: @odata.nextLink
        if (root.has("@odata.nextLink")) return root.get("@odata.nextLink").asText();
        return null;
    }

    /** Extracts the path portion from a full or relative next-link URL. */
    private String extractPath(String nextLink, String fallbackPath) {
        if (nextLink.startsWith("http://") || nextLink.startsWith("https://")) {
            int idx = nextLink.indexOf('/', 8);
            if (idx > 0) return nextLink.substring(idx);
            return fallbackPath;
        }
        int qIdx = nextLink.indexOf('?');
        return qIdx > 0 ? nextLink.substring(0, qIdx) : nextLink;
    }

    /** Extracts query parameters from a next-link URL into a map. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractQuery(String nextLink) {
        Map<String, Object> result = new HashMap<>();
        int qIdx = nextLink.indexOf('?');
        if (qIdx < 0 || qIdx >= nextLink.length() - 1) return result;
        String queryStr = nextLink.substring(qIdx + 1);
        for (String pair : queryStr.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                try { result.put(pair.substring(0, eq), java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8")); }
                catch (Exception ignored) { }
            }
        }
        return result;
    }
    private void requireEnabled() { if (!properties.isEnabled()) throw new BizException(5031, "integration.disabled"); }
    private String pathOrDefault(String path, String fallback) { return path == null || path.trim().isEmpty() ? fallback : path.trim(); }
    private Map<String, ?> withPageSize(Map<String, ?> query) { Map<String, Object> value = new HashMap<>(); if (query != null) value.putAll(query); value.putIfAbsent("$top", properties.getPageSize()); return value; }
    private String recentChangeFilter(String field) {
        // SAP time is UTC; subtract 8 hours to align with China timezone (UTC+8)
        // so that changes made in the last N minutes in China time are captured.
        String since = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(15).minusHours(8).truncatedTo(ChronoUnit.SECONDS).toString();
        return field + " ge datetimeoffset'" + since + "'";
    }

    /** Work order filter: Plant eq 'TK10' AND (created recently OR changed recently).
     *  Note: LastChangeDateTime in A_ProductionOrder_2 is a string (yyyyMMddHHmmss), not datetimeoffset. */
    private String workOrderRecentFilter() {
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(15).minusHours(8).truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime until = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).truncatedTo(ChronoUnit.SECONDS);
        String sinceDate = since.toLocalDate().toString() + "T00:00:00";
        String untilDate = until.toLocalDate().toString() + "T00:00:00";
        // LastChangeDateTime is string format yyyyMMddHHmmss
        String sinceStr = since.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String untilStr = until.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "(Plant eq 'TK10') and ((MfgOrderCreationDate ge datetime'" + sinceDate + "' and MfgOrderCreationDate lt datetime'" + untilDate + "') or (LastChangeDateTime ge '" + sinceStr + "' and LastChangeDateTime lt '" + untilStr + "'))";
    }

    /** Convert a LastChangeDateTime filter from datetimeoffset format to plain string format (yyyyMMddHHmmss)
     *  for use with A_ProductionOrder_2 where LastChangeDateTime is a string field. */
    private String convertWoLastChangeFilter(String filter) {
        if (filter == null || !filter.contains("LastChangeDateTime")) return filter;
        // Match pattern: LastChangeDateTime ge datetimeoffset'YYYY-MM-DDTHH:mm:ssZ'
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "LastChangeDateTime\\s+(ge|gt|le|lt|eq)\\s+datetimeoffset'([^']+)'");
        java.util.regex.Matcher m = p.matcher(filter);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String op = m.group(1);
            String dateStr = m.group(2);
            try {
                OffsetDateTime dt = OffsetDateTime.parse(dateStr);
                String fmt = dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                m.appendReplacement(sb, "LastChangeDateTime " + op + " '" + fmt + "'");
            } catch (Exception ex) {
                m.appendReplacement(sb, m.group(0));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
    private Map<String, String> authHeaders() { Map<String, String> h = new HashMap<>(); if (properties.getBearerToken() != null && !properties.getBearerToken().trim().isEmpty()) h.put(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getBearerToken().trim()); else if (properties.getUsername() != null && properties.getPassword() != null) h.put(HttpHeaders.AUTHORIZATION, "Basic " + java.util.Base64.getEncoder().encodeToString((properties.getUsername() + ":" + properties.getPassword()).getBytes(java.nio.charset.StandardCharsets.UTF_8))); h.put(HttpHeaders.ACCEPT, "application/json"); return h; }
    private List<JsonNode> rows(JsonNode root) { JsonNode value = root; for (String key : new String[]{"value", "data", "items", "results"}) if (value.isObject() && value.has(key)) { value = value.get(key); break; } if (value.isObject() && value.has("d")) { value = value.get("d"); if (value.isObject() && value.has("results")) value = value.get("results"); } List<JsonNode> list = new ArrayList<>(); if (value.isArray()) value.forEach(list::add); else if (value.isObject() && !value.isMissingNode()) list.add(value); return list; }
    private String text(JsonNode node, String... names) { for (String name : names) if (node.hasNonNull(name)) return node.get(name).asText(); return null; }
    /** Extract plant from to_BatchPlant navigation property if available. */
    private String plantFromBatchExpand(JsonNode row) {
        List<JsonNode> plants = childRows(row, "to_BatchPlant");
        for (JsonNode p : plants) {
            String v = text(p, "Plant", "plant");
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private String first(String... values) { for (String value : values) if (value != null && !value.trim().isEmpty()) return value; return null; }
    private BigDecimal firstDecimal(BigDecimal value, BigDecimal fallback) { return value == null ? fallback : value; }
    private BigDecimal decimal(JsonNode node, String... names) { String v = text(node, names); try { return v == null ? null : new BigDecimal(v.replace(",", "").trim()); } catch (Exception ex) { return null; } }
    private int integer(JsonNode node, int fallback, String... names) { String v = text(node, names); try { return v == null ? fallback : Integer.parseInt(v); } catch (Exception ex) { return fallback; } }
    private boolean bool(JsonNode node, boolean fallback, String... names) { String v = text(node, names); return v == null ? fallback : !("false".equalsIgnoreCase(v) || "0".equals(v) || "N".equalsIgnoreCase(v)); }
    private boolean bool(Object value) { if (value == null) return false; String s = String.valueOf(value).trim(); return "true".equalsIgnoreCase(s) || "X".equals(s) || "1".equals(s); }
    private Boolean boolObject(JsonNode node, String... names) { String value = text(node, names); return value == null ? null : bool(node, false, names); }
    private LocalDate date(JsonNode node, String... names) {
        String value = text(node, names);
        if (value == null || value.trim().isEmpty()) return null;
        try { return OffsetDateTime.parse(value).toLocalDate(); } catch (DateTimeParseException ignored) { }
        try { return LocalDateTime.parse(value).toLocalDate(); } catch (DateTimeParseException ignored) { }
        try { return LocalDate.parse(value); } catch (DateTimeParseException ignored) { }
        return null;
    }
    private LocalDateTime dateTime(JsonNode node, String... names) {
        String value = text(node, names);
        if (value == null || value.trim().isEmpty()) return null;
        try { return LocalDateTime.parse(value); } catch (DateTimeParseException ignored) { }
        try { return OffsetDateTime.parse(value).toLocalDateTime(); } catch (DateTimeParseException ignored) { }
        try { return LocalDate.parse(value).atStartOfDay(); } catch (DateTimeParseException ignored) { }
        try { return LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")); }
        catch (DateTimeParseException ignored) { }
        return null;
    }
    private String productDescription(JsonNode row) {
        Map<String, String> descs = productDescriptionsByLanguage(row);
        return first(descs.get("ZH"), descs.get("EN"), descs.values().stream().findFirst().orElse(null));
    }

    /**
     * 从 SAP OData 响应中按语言提取产品描述。
     * 数据来源:
     *   1) to_Description → A_ProductDescriptionType.ProductDescription (短描述, MaxLength=40)
     *   2) to_BasicText   → A_ProductBasicTextType.LongText          (长文本, 无长度限制)
     * LongText 更详细，优先覆盖 ProductDescription。
     *
     * @return Map<LanguageCode, Description>  例如 {"ZH": "产品描述", "EN": "Product Desc"}
     */
    private Map<String, String> productDescriptionsByLanguage(JsonNode row) {
        Map<String, String> result = new LinkedHashMap<>();
        // 1. 从 to_Description 提取短描述 (ProductDescription)
        List<JsonNode> descriptions = childRows(row, "to_Description");
        for (JsonNode desc : descriptions) {
            String language = text(desc, "Language");
            String value = text(desc, "ProductDescription", "Description");
            if (language != null && value != null && !value.trim().isEmpty()) {
                result.putIfAbsent(language.toUpperCase(), value);
            }
        }
        // 2. 从 to_BasicText 提取长文本 (LongText)
        //    LongText 更详细，直接覆盖短描述
        List<JsonNode> basicTexts = childRows(row, "to_BasicText");
        for (JsonNode bt : basicTexts) {
            String language = text(bt, "Language");
            String value = text(bt, "LongText");
            if (language != null && value != null && !value.trim().isEmpty()) {
                result.put(language.toUpperCase(), value);
            }
        }
        return result;
    }
    private String normalizeStatus(String value) { return value == null || value.trim().isEmpty() || "true".equalsIgnoreCase(value) || "1".equals(value) ? "ACTIVE" : ("false".equalsIgnoreCase(value) || "0".equals(value) ? "INACTIVE" : value.toUpperCase()); }
    private String workOrderStatus(JsonNode row) { String value = text(row, "status", "Status", "orderStatus", "OrderStatus", "SystemStatus"); if (bool(row, false, "MfgOrderIsTechnicallyCompleted", "IsTechnicallyCompleted")) return "COMPLETED"; if (value == null && bool(row, false, "MfgOrderIsReleased", "IsReleased")) return "RELEASED"; if (value == null) return "DRAFT"; String v = value.toUpperCase(); if (v.contains("COMPLETE")) return "COMPLETED"; if (v.contains("PROGRESS")) return "IN_PROGRESS"; if (v.contains("RELEASE")) return "RELEASED"; if (v.contains("CANCEL")) return "CANCELLED"; return "DRAFT"; }
    private Map<String, Object> mapOf(String a, Object b, String c, Object d) { Map<String, Object> m = new HashMap<>(); m.put(a, b); m.put(c, d); return m; }

    public static class SyncResult { private final String entity; private final int received, created, updated, failed; private final List<String> errors; public SyncResult(String entity, int received, int created, int updated, int failed, List<String> errors) { this.entity=entity; this.received=received; this.created=created; this.updated=updated; this.failed=failed; this.errors=errors; } public String getEntity(){return entity;} public int getReceived(){return received;} public int getCreated(){return created;} public int getUpdated(){return updated;} public int getFailed(){return failed;} public List<String> getErrors(){return errors;} }
}
