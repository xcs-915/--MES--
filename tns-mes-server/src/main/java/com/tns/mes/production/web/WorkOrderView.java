package com.tns.mes.production.web;

import com.tns.mes.engineering.domain.BomItem;
import com.tns.mes.engineering.domain.ProcessOperation;
import com.tns.mes.engineering.domain.Product;
import com.tns.mes.engineering.repo.ProductRepository;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.domain.WorkOrderOperation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkOrderView {
    private Long id;
    private String orderNo;
    private Long productId;
    private String productCode;
    private String productNameZh;
    private String productNameEn;
    private String productNameAr;
    private Long bomId;
    private String bomCode;
    private Long routeId;
    private String routeCode;
    private Long factoryId;
    private Long workshopId;
    private BigDecimal quantity;
    private BigDecimal completedQuantity;
    private Integer priority;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private String status;
    private String source;
    private String remark;
    private String orderCategory;
    private String orderType;
    private String productionPlant;
    private String plant;
    private String storageLocation;
    private String mrpArea;
    private String mrpController;
    private String productionSupervisor;
    private String productionVersion;
    private String plannedOrder;
    private String salesOrder;
    private String salesOrderItem;
    private String companyCode;
    private String profitCenter;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDate actualReleaseDate;
    private String productionUnit;
    private BigDecimal plannedScrapQuantity;
    private BigDecimal confirmedYieldQuantity;
    private String orderLongText;
    private Boolean locked;
    private Boolean markedForDeletion;
    private LocalDateTime sapCreatedAt;
    private LocalDateTime sapChangedAt;
    private LocalDateTime sapLastSyncAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ComponentView> components = Collections.emptyList();
    private List<OperationView> operations = Collections.emptyList();
    // Planned order + SAP extended fields
    private String plannedOrderType;
    private String plannedOrderProfile;
    private String materialName;
    private String mrpPlant;
    private String materialProcurementCategory;
    private String materialProcurementType;
    private BigDecimal plannedScrapQtySap;
    private BigDecimal goodsReceiptQty;
    private BigDecimal issuedQuantity;
    private LocalDateTime plannedOrderOpeningDate;
    private LocalDateTime productionStartDate;
    private LocalDateTime productionEndDate;
    private String customer;
    private String wbsElement;
    private String wbsDescription;
    private String purchasingGroup;
    private String purchasingOrganization;
    private String fixedSupplier;
    private String supplierName;
    private Boolean plannedOrderIsFirm;
    private Boolean plannedOrderIsConvertible;
    private String schedulingType;

    public WorkOrderView(WorkOrder value) {
        mapSummary(value);
    }

    public WorkOrderView(WorkOrder value, ProductRepository products) {
        mapSummary(value);
        mapDetails(value, products);
    }

    private void mapSummary(WorkOrder value) {
        id=value.getId(); orderNo=value.getOrderNo();
        if(value.getProduct()!=null){productId=value.getProduct().getId();productCode=value.getProduct().getCode();productNameZh=value.getProduct().getNameZh();productNameEn=value.getProduct().getNameEn();productNameAr=value.getProduct().getNameAr();}
        if(value.getBom()!=null){bomId=value.getBom().getId();bomCode=value.getBom().getCode();}
        if(value.getRoute()!=null){routeId=value.getRoute().getId();routeCode=value.getRoute().getCode();}
        factoryId=value.getFactoryId(); workshopId=value.getWorkshopId(); quantity=value.getQuantity(); completedQuantity=value.getCompletedQuantity();
        priority=value.getPriority(); plannedStart=value.getPlannedStart(); plannedEnd=value.getPlannedEnd(); status=value.getStatus(); source=value.getSource(); remark=value.getRemark();
        orderCategory=value.getOrderCategory(); orderType=value.getOrderType(); productionPlant=value.getProductionPlant(); plant=value.getPlant(); storageLocation=value.getStorageLocation();
        mrpArea=value.getMrpArea(); mrpController=value.getMrpController(); productionSupervisor=value.getProductionSupervisor(); productionVersion=value.getProductionVersion();
        plannedOrder=value.getPlannedOrder(); salesOrder=value.getSalesOrder(); salesOrderItem=value.getSalesOrderItem(); companyCode=value.getCompanyCode(); profitCenter=value.getProfitCenter();
        scheduledStart=value.getScheduledStart(); scheduledEnd=value.getScheduledEnd(); actualReleaseDate=value.getActualReleaseDate(); productionUnit=value.getProductionUnit();
        plannedScrapQuantity=value.getPlannedScrapQuantity(); confirmedYieldQuantity=value.getConfirmedYieldQuantity(); orderLongText=value.getOrderLongText(); locked=value.getLocked();
        markedForDeletion=value.getMarkedForDeletion(); sapCreatedAt=value.getSapCreatedAt(); sapChangedAt=value.getSapChangedAt(); sapLastSyncAt=value.getSapLastSyncAt();
        createdAt=value.getCreatedAt(); updatedAt=value.getUpdatedAt();
        plannedOrderType=value.getPlannedOrderType(); plannedOrderProfile=value.getPlannedOrderProfile(); materialName=value.getMaterialName(); mrpPlant=value.getMrpPlant();
        materialProcurementCategory=value.getMaterialProcurementCategory(); materialProcurementType=value.getMaterialProcurementType(); plannedScrapQtySap=value.getPlannedScrapQtySap();
        goodsReceiptQty=value.getGoodsReceiptQty(); issuedQuantity=value.getIssuedQuantity(); plannedOrderOpeningDate=value.getPlannedOrderOpeningDate();
        productionStartDate=value.getProductionStartDate(); productionEndDate=value.getProductionEndDate(); customer=value.getCustomer();
        wbsElement=value.getWbsElement(); wbsDescription=value.getWbsDescription(); purchasingGroup=value.getPurchasingGroup();
        purchasingOrganization=value.getPurchasingOrganization(); fixedSupplier=value.getFixedSupplier(); supplierName=value.getSupplierName();
        plannedOrderIsFirm=value.getPlannedOrderIsFirm(); plannedOrderIsConvertible=value.getPlannedOrderIsConvertible(); schedulingType=value.getSchedulingType();
    }

    private void mapDetails(WorkOrder value, ProductRepository products) {
        if (value.getBom() != null) {
            List<BomItem> items = value.getBom().getItems();
            List<Long> productIds = items.stream().map(BomItem::getComponentProductId).distinct().collect(Collectors.toList());
            Map<Long, Product> productMap = new HashMap<>();
            products.findAllById(productIds).forEach(product -> productMap.put(product.getId(), product));
            components = items.stream().map(item -> new ComponentView(item, productMap.get(item.getComponentProductId()))).collect(Collectors.toList());
        }
        if (!value.getOperations().isEmpty()) {
            operations = value.getOperations().stream().map(OperationView::new).collect(Collectors.toList());
        } else if (value.getRoute() != null) {
            operations = value.getRoute().getOperations().stream().map(operation -> new OperationView(operation, value.getQuantity())).collect(Collectors.toList());
        }
    }

    public Long getId(){return id;} public String getOrderNo(){return orderNo;} public Long getProductId(){return productId;}
    public String getProductCode(){return productCode;} public String getProductNameZh(){return productNameZh;} public String getProductNameEn(){return productNameEn;}
    public String getProductNameAr(){return productNameAr;} public Long getBomId(){return bomId;} public String getBomCode(){return bomCode;} public Long getRouteId(){return routeId;}
    public String getRouteCode(){return routeCode;} public Long getFactoryId(){return factoryId;} public Long getWorkshopId(){return workshopId;} public BigDecimal getQuantity(){return quantity;}
    public BigDecimal getCompletedQuantity(){return completedQuantity;} public Integer getPriority(){return priority;} public LocalDateTime getPlannedStart(){return plannedStart;}
    public LocalDateTime getPlannedEnd(){return plannedEnd;} public String getStatus(){return status;} public String getSource(){return source;} public String getRemark(){return remark;}
    public String getOrderCategory(){return orderCategory;} public String getOrderType(){return orderType;} public String getProductionPlant(){return productionPlant;} public String getPlant(){return plant;}
    public String getStorageLocation(){return storageLocation;} public String getMrpArea(){return mrpArea;} public String getMrpController(){return mrpController;} public String getProductionSupervisor(){return productionSupervisor;}
    public String getProductionVersion(){return productionVersion;} public String getPlannedOrder(){return plannedOrder;} public String getSalesOrder(){return salesOrder;} public String getSalesOrderItem(){return salesOrderItem;}
    public String getCompanyCode(){return companyCode;} public String getProfitCenter(){return profitCenter;} public LocalDateTime getScheduledStart(){return scheduledStart;} public LocalDateTime getScheduledEnd(){return scheduledEnd;}
    public LocalDate getActualReleaseDate(){return actualReleaseDate;} public String getProductionUnit(){return productionUnit;} public BigDecimal getPlannedScrapQuantity(){return plannedScrapQuantity;}
    public BigDecimal getConfirmedYieldQuantity(){return confirmedYieldQuantity;} public String getOrderLongText(){return orderLongText;} public Boolean getLocked(){return locked;} public Boolean getMarkedForDeletion(){return markedForDeletion;}
    public LocalDateTime getSapCreatedAt(){return sapCreatedAt;} public LocalDateTime getSapChangedAt(){return sapChangedAt;} public LocalDateTime getSapLastSyncAt(){return sapLastSyncAt;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public List<ComponentView> getComponents(){return components;} public List<OperationView> getOperations(){return operations;}
    public String getPlannedOrderType(){return plannedOrderType;} public String getPlannedOrderProfile(){return plannedOrderProfile;} public String getMaterialName(){return materialName;} public String getMrpPlant(){return mrpPlant;}
    public String getMaterialProcurementCategory(){return materialProcurementCategory;} public String getMaterialProcurementType(){return materialProcurementType;} public BigDecimal getPlannedScrapQtySap(){return plannedScrapQtySap;}
    public BigDecimal getGoodsReceiptQty(){return goodsReceiptQty;} public BigDecimal getIssuedQuantity(){return issuedQuantity;} public LocalDateTime getPlannedOrderOpeningDate(){return plannedOrderOpeningDate;}
    public LocalDateTime getProductionStartDate(){return productionStartDate;} public LocalDateTime getProductionEndDate(){return productionEndDate;} public String getCustomer(){return customer;}
    public String getWbsElement(){return wbsElement;} public String getWbsDescription(){return wbsDescription;} public String getPurchasingGroup(){return purchasingGroup;}
    public String getPurchasingOrganization(){return purchasingOrganization;} public String getFixedSupplier(){return fixedSupplier;} public String getSupplierName(){return supplierName;}
    public Boolean getPlannedOrderIsFirm(){return plannedOrderIsFirm;} public Boolean getPlannedOrderIsConvertible(){return plannedOrderIsConvertible;} public String getSchedulingType(){return schedulingType;}

    public static class ComponentView {
        private final Long id; private final Integer sequenceNo; private final String productCode; private final String productName; private final String materialGroup;
        private final BigDecimal requiredQuantity; private final BigDecimal withdrawnQuantity; private final BigDecimal availableQuantity; private final String unit;
        private final String reservationNo; private final String reservationItem; private final String operationCode; private final LocalDate requirementDate;
        private final String storageLocation; private final String goodsMovementType; private final String bomItemNumber; private final String itemDescription;
        private final Boolean bulkMaterial; private final Boolean backflush; private final Boolean deleted;
        private final String batch; private final String manufacturingOrderOperation;
        ComponentView(BomItem value, Product product) {
            id=value.getId(); sequenceNo=value.getSequenceNo(); productCode=product==null?String.valueOf(value.getComponentProductId()):product.getCode();
            productName=product==null?null:product.getNameZh(); materialGroup=value.getMaterialGroup(); requiredQuantity=value.getQuantity(); withdrawnQuantity=value.getWithdrawnQuantity();
            availableQuantity=value.getAvailableQuantity(); unit=value.getUnit(); reservationNo=value.getReservationNo(); reservationItem=value.getReservationItem(); operationCode=value.getOperationCode();
            requirementDate=value.getRequirementDate(); storageLocation=value.getStorageLocation(); goodsMovementType=value.getGoodsMovementType(); bomItemNumber=value.getBomItemNumber(); itemDescription=value.getItemDescription();
            bulkMaterial=value.getBulkMaterial(); backflush=value.getBackflush(); deleted=value.getDeleted();
            batch=value.getBatch(); manufacturingOrderOperation=value.getManufacturingOrderOperation();
        }
        public Long getId(){return id;} public Integer getSequenceNo(){return sequenceNo;} public String getProductCode(){return productCode;} public String getProductName(){return productName;}
        public String getMaterialGroup(){return materialGroup;} public BigDecimal getRequiredQuantity(){return requiredQuantity;} public BigDecimal getWithdrawnQuantity(){return withdrawnQuantity;}
        public BigDecimal getAvailableQuantity(){return availableQuantity;} public String getUnit(){return unit;} public String getReservationNo(){return reservationNo;} public String getReservationItem(){return reservationItem;}
        public String getOperationCode(){return operationCode;} public LocalDate getRequirementDate(){return requirementDate;} public String getStorageLocation(){return storageLocation;} public String getGoodsMovementType(){return goodsMovementType;}
        public String getBomItemNumber(){return bomItemNumber;} public String getItemDescription(){return itemDescription;} public Boolean getBulkMaterial(){return bulkMaterial;} public Boolean getBackflush(){return backflush;} public Boolean getDeleted(){return deleted;}
        public String getBatch(){return batch;} public String getManufacturingOrderOperation(){return manufacturingOrderOperation;}
    }

    public static class OperationView {
        private Long id; private Long operationId; private Integer sequenceNo; private String status; private BigDecimal plannedQuantity; private BigDecimal completedQuantity;
        private String operationCode; private String operationNameZh; private String operationNameEn; private String operationNameAr; private String plant; private String workCenterCode;
        private String controlProfile; private String operationUnit; private BigDecimal plannedYieldQuantity; private BigDecimal plannedScrapQuantity; private BigDecimal confirmedYieldQuantity;
        private BigDecimal confirmedScrapQuantity; private LocalDateTime earliestStart; private LocalDateTime earliestEnd; private LocalDateTime latestStart; private LocalDateTime latestEnd;
        private String operationName; private String workCenterDesc; private String controlKey; private BigDecimal plannedTotalQuantity;
        OperationView(WorkOrderOperation value){
            id=value.getId();operationId=value.getOperation()==null?null:value.getOperation().getId();
            sequenceNo=value.getSequenceNo();status=value.getStatus();plannedQuantity=value.getPlannedQuantity();completedQuantity=value.getCompletedQuantity();
            // Read from WorkOrderOperation's own fields first
            operationCode=value.getOperationCode();operationName=value.getOperationName();operationNameZh=value.getOperationName();
            workCenterCode=value.getWorkCenterCode();workCenterDesc=value.getWorkCenterDesc();plant=value.getPlant();
            controlKey=value.getControlKey();controlProfile=value.getControlKey();operationUnit=value.getOperationUnit();
            plannedYieldQuantity=value.getPlannedYieldQuantity();confirmedYieldQuantity=value.getConfirmedYieldQuantity();
            plannedTotalQuantity=value.getPlannedTotalQuantity();
            // Fall back to ProcessOperation if available and own fields are null
            if(value.getOperation()!=null) mapOperation(value.getOperation());
        }
        OperationView(ProcessOperation value, BigDecimal orderQuantity){id=value.getId();operationId=value.getId();sequenceNo=value.getSequenceNo();status="PENDING";plannedQuantity=orderQuantity;completedQuantity=value.getConfirmedYieldQuantity();mapOperation(value);}
        private void mapOperation(ProcessOperation value){if(value==null)return;
            if(operationCode==null) operationCode=value.getCode();
            if(operationNameZh==null) operationNameZh=value.getNameZh();
            if(operationNameEn==null) operationNameEn=value.getNameEn();
            if(operationNameAr==null) operationNameAr=value.getNameAr();
            if(plant==null) plant=value.getPlant();
            if(workCenterCode==null) workCenterCode=value.getWorkCenterCode();
            if(controlProfile==null) controlProfile=value.getControlProfile();
            if(operationUnit==null) operationUnit=value.getOperationUnit();
            if(plannedYieldQuantity==null) plannedYieldQuantity=value.getPlannedYieldQuantity();
            if(plannedScrapQuantity==null) plannedScrapQuantity=value.getPlannedScrapQuantity();
            if(confirmedYieldQuantity==null) confirmedYieldQuantity=value.getConfirmedYieldQuantity();
            if(confirmedScrapQuantity==null) confirmedScrapQuantity=value.getConfirmedScrapQuantity();
            earliestStart=value.getEarliestStart();earliestEnd=value.getEarliestEnd();latestStart=value.getLatestStart();latestEnd=value.getLatestEnd();
        }
        public Long getId(){return id;} public Long getOperationId(){return operationId;} public Integer getSequenceNo(){return sequenceNo;} public String getStatus(){return status;} public BigDecimal getPlannedQuantity(){return plannedQuantity;}
        public BigDecimal getCompletedQuantity(){return completedQuantity;} public String getOperationCode(){return operationCode;} public String getOperationNameZh(){return operationNameZh;} public String getOperationNameEn(){return operationNameEn;}
        public String getOperationNameAr(){return operationNameAr;} public String getPlant(){return plant;} public String getWorkCenterCode(){return workCenterCode;} public String getControlProfile(){return controlProfile;}
        public String getOperationUnit(){return operationUnit;} public BigDecimal getPlannedYieldQuantity(){return plannedYieldQuantity;} public BigDecimal getPlannedScrapQuantity(){return plannedScrapQuantity;}
        public BigDecimal getConfirmedYieldQuantity(){return confirmedYieldQuantity;} public BigDecimal getConfirmedScrapQuantity(){return confirmedScrapQuantity;} public LocalDateTime getEarliestStart(){return earliestStart;}
        public LocalDateTime getEarliestEnd(){return earliestEnd;} public LocalDateTime getLatestStart(){return latestStart;} public LocalDateTime getLatestEnd(){return latestEnd;}
        public String getOperationName(){return operationName;} public String getWorkCenterDesc(){return workCenterDesc;} public String getControlKey(){return controlKey;} public BigDecimal getPlannedTotalQuantity(){return plannedTotalQuantity;}
    }
}
