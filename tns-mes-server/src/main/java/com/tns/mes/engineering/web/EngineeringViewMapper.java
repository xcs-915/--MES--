package com.tns.mes.engineering.web;

import com.tns.mes.engineering.domain.Bom;
import com.tns.mes.engineering.domain.BomItem;
import com.tns.mes.engineering.domain.InspectionItem;
import com.tns.mes.engineering.domain.InspectionRule;
import com.tns.mes.engineering.domain.ProcessOperation;
import com.tns.mes.engineering.domain.ProcessRoute;
import com.tns.mes.engineering.domain.Product;

import java.util.List;
import java.util.stream.Collectors;

public final class EngineeringViewMapper {
    private EngineeringViewMapper() { }
    public static ProductView product(Product value) { return new ProductView(value); }
    public static BomView bom(Bom value) { return new BomView(value); }
    public static RouteView route(ProcessRoute value) { return new RouteView(value); }
    public static InspectionView inspection(InspectionRule value) { return new InspectionView(value); }

    public static class ProductView {
        private Long id; private String code; private String nameZh; private String nameEn; private String nameAr; private String productType; private String unit; private String specification; private String status; private Boolean traceable; private Long version;
        private String source; private String productOldId; private String productGroup; private java.math.BigDecimal grossWeight; private java.math.BigDecimal netWeight; private String weightUnit;
        private String countryOfOrigin; private String productHierarchy; private String divisionCode; private String manufacturerNumber; private String manufacturerPartNumber; private String materialRevisionLevel;
        private String serialNumberProfile; private Boolean batchManaged; private Boolean markedForDeletion; private String brand; private String color; private String customerPartNumber; private String productModel;
        private String drawingNumber; private java.math.BigDecimal minPackagingQty; private java.time.LocalDateTime sapCreatedAt; private java.time.LocalDateTime sapChangedAt; private java.time.LocalDateTime sapLastSyncAt; private java.time.Instant createdAt; private java.time.Instant updatedAt;
        ProductView(Product v) { id=v.getId(); code=v.getCode(); nameZh=v.getNameZh(); nameEn=v.getNameEn(); nameAr=v.getNameAr(); productType=v.getProductType(); unit=v.getUnit(); specification=v.getSpecification(); status=v.getStatus(); traceable=v.getTraceable(); version=v.getVersion(); source=v.getSource(); productOldId=v.getProductOldId(); productGroup=v.getProductGroup(); grossWeight=v.getGrossWeight(); netWeight=v.getNetWeight(); weightUnit=v.getWeightUnit(); countryOfOrigin=v.getCountryOfOrigin(); productHierarchy=v.getProductHierarchy(); divisionCode=v.getDivisionCode(); manufacturerNumber=v.getManufacturerNumber(); manufacturerPartNumber=v.getManufacturerPartNumber(); materialRevisionLevel=v.getMaterialRevisionLevel(); serialNumberProfile=v.getSerialNumberProfile(); batchManaged=v.getBatchManaged(); markedForDeletion=v.getMarkedForDeletion(); brand=v.getBrand(); color=v.getColor(); customerPartNumber=v.getCustomerPartNumber(); productModel=v.getProductModel(); drawingNumber=v.getDrawingNumber(); minPackagingQty=v.getMinPackagingQty(); sapCreatedAt=v.getSapCreatedAt(); sapChangedAt=v.getSapChangedAt(); sapLastSyncAt=v.getSapLastSyncAt(); createdAt=v.getCreatedAt(); updatedAt=v.getUpdatedAt(); }
        public Long getId(){return id;} public String getCode(){return code;} public String getNameZh(){return nameZh;} public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public String getProductType(){return productType;} public String getUnit(){return unit;} public String getSpecification(){return specification;} public String getStatus(){return status;} public Boolean getTraceable(){return traceable;} public Long getVersion(){return version;}
        public String getSource(){return source;} public String getProductOldId(){return productOldId;} public String getProductGroup(){return productGroup;} public java.math.BigDecimal getGrossWeight(){return grossWeight;} public java.math.BigDecimal getNetWeight(){return netWeight;} public String getWeightUnit(){return weightUnit;} public String getCountryOfOrigin(){return countryOfOrigin;} public String getProductHierarchy(){return productHierarchy;} public String getDivisionCode(){return divisionCode;} public String getManufacturerNumber(){return manufacturerNumber;} public String getManufacturerPartNumber(){return manufacturerPartNumber;} public String getMaterialRevisionLevel(){return materialRevisionLevel;} public String getSerialNumberProfile(){return serialNumberProfile;} public Boolean getBatchManaged(){return batchManaged;} public Boolean getMarkedForDeletion(){return markedForDeletion;} public String getBrand(){return brand;} public String getColor(){return color;} public String getCustomerPartNumber(){return customerPartNumber;} public String getProductModel(){return productModel;} public String getDrawingNumber(){return drawingNumber;} public java.math.BigDecimal getMinPackagingQty(){return minPackagingQty;} public java.time.LocalDateTime getSapCreatedAt(){return sapCreatedAt;} public java.time.LocalDateTime getSapChangedAt(){return sapChangedAt;} public java.time.LocalDateTime getSapLastSyncAt(){return sapLastSyncAt;} public java.time.Instant getCreatedAt(){return createdAt;} public java.time.Instant getUpdatedAt(){return updatedAt;}
    }
    public static class BomView {
        private Long id; private Long productId; private String code; private String versionCode; private String nameZh; private String nameEn; private String nameAr; private String status; private java.time.LocalDate effectiveFrom; private java.time.LocalDate effectiveTo; private List<BomItemView> items;
        BomView(Bom v) { id=v.getId(); productId=v.getProductId(); code=v.getCode(); versionCode=v.getVersionCode(); nameZh=v.getNameZh(); nameEn=v.getNameEn(); nameAr=v.getNameAr(); status=v.getStatus(); effectiveFrom=v.getEffectiveFrom(); effectiveTo=v.getEffectiveTo(); items=v.getItems().stream().map(BomItemView::new).collect(Collectors.toList()); }
        public Long getId(){return id;} public Long getProductId(){return productId;} public String getCode(){return code;} public String getVersionCode(){return versionCode;} public String getNameZh(){return nameZh;} public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public String getStatus(){return status;} public java.time.LocalDate getEffectiveFrom(){return effectiveFrom;} public java.time.LocalDate getEffectiveTo(){return effectiveTo;} public List<BomItemView> getItems(){return items;}
    }
    public static class BomItemView {
        private Long id; private Long componentProductId; private Integer sequenceNo; private java.math.BigDecimal quantity; private java.math.BigDecimal scrapRate; private String unit; private String issueMethod;
        BomItemView(BomItem v) { id=v.getId(); componentProductId=v.getComponentProductId(); sequenceNo=v.getSequenceNo(); quantity=v.getQuantity(); scrapRate=v.getScrapRate(); unit=v.getUnit(); issueMethod=v.getIssueMethod(); }
        public Long getId(){return id;} public Long getComponentProductId(){return componentProductId;} public Integer getSequenceNo(){return sequenceNo;} public java.math.BigDecimal getQuantity(){return quantity;} public java.math.BigDecimal getScrapRate(){return scrapRate;} public String getUnit(){return unit;} public String getIssueMethod(){return issueMethod;}
    }
    public static class RouteView {
        private Long id; private Long productId; private String code; private String versionCode; private String nameZh; private String nameEn; private String nameAr; private String status; private List<OperationView> operations;
        RouteView(ProcessRoute v) { id=v.getId(); productId=v.getProductId(); code=v.getCode(); versionCode=v.getVersionCode(); nameZh=v.getNameZh(); nameEn=v.getNameEn(); nameAr=v.getNameAr(); status=v.getStatus(); operations=v.getOperations().stream().map(OperationView::new).collect(Collectors.toList()); }
        public Long getId(){return id;} public Long getProductId(){return productId;} public String getCode(){return code;} public String getVersionCode(){return versionCode;} public String getNameZh(){return nameZh;} public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public String getStatus(){return status;} public List<OperationView> getOperations(){return operations;}
    }
    public static class OperationView {
        private Long id; private Integer sequenceNo; private String code; private String nameZh; private String nameEn; private String nameAr; private Long workCenterId; private Integer standardTimeSeconds; private Integer queueTimeSeconds; private Boolean inspection;
        OperationView(ProcessOperation v) { id=v.getId(); sequenceNo=v.getSequenceNo(); code=v.getCode(); nameZh=v.getNameZh(); nameEn=v.getNameEn(); nameAr=v.getNameAr(); workCenterId=v.getWorkCenterId(); standardTimeSeconds=v.getStandardTimeSeconds(); queueTimeSeconds=v.getQueueTimeSeconds(); inspection=v.getInspection(); }
        public Long getId(){return id;} public Integer getSequenceNo(){return sequenceNo;} public String getCode(){return code;} public String getNameZh(){return nameZh;} public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public Long getWorkCenterId(){return workCenterId;} public Integer getStandardTimeSeconds(){return standardTimeSeconds;} public Integer getQueueTimeSeconds(){return queueTimeSeconds;} public Boolean getInspection(){return inspection;}
    }
    public static class InspectionView {
        private Long id; private String code; private String nameZh; private String nameEn; private String nameAr; private String inspectionType; private String samplingMethod; private String status; private List<InspectionItemView> items;
        InspectionView(InspectionRule v) { id=v.getId(); code=v.getCode(); nameZh=v.getNameZh(); nameEn=v.getNameEn(); nameAr=v.getNameAr(); inspectionType=v.getInspectionType(); samplingMethod=v.getSamplingMethod(); status=v.getStatus(); items=v.getItems().stream().map(InspectionItemView::new).collect(Collectors.toList()); }
        public Long getId(){return id;} public String getCode(){return code;} public String getNameZh(){return nameZh;} public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public String getInspectionType(){return inspectionType;} public String getSamplingMethod(){return samplingMethod;} public String getStatus(){return status;} public List<InspectionItemView> getItems(){return items;}
    }
    public static class InspectionItemView {
        private Long id; private Integer sequenceNo; private String code; private String nameZh; private String nameEn; private String nameAr; private String specification; private String unit; private java.math.BigDecimal minValue; private java.math.BigDecimal maxValue; private String dataType; private Boolean mandatory;
        InspectionItemView(InspectionItem v) { id=v.getId(); sequenceNo=v.getSequenceNo(); code=v.getCode(); nameZh=v.getNameZh(); nameEn=v.getNameEn(); nameAr=v.getNameAr(); specification=v.getSpecification(); unit=v.getUnit(); minValue=v.getMinValue(); maxValue=v.getMaxValue(); dataType=v.getDataType(); mandatory=v.getMandatory(); }
        public Long getId(){return id;} public Integer getSequenceNo(){return sequenceNo;} public String getCode(){return code;} public String getNameZh(){return nameZh;} public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public String getSpecification(){return specification;} public String getUnit(){return unit;} public java.math.BigDecimal getMinValue(){return minValue;} public java.math.BigDecimal getMaxValue(){return maxValue;} public String getDataType(){return dataType;} public Boolean getMandatory(){return mandatory;}
    }
}
