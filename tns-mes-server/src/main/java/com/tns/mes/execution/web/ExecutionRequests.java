package com.tns.mes.execution.web;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ExecutionRequests {
    private ExecutionRequests() { }

    public static class MaterialIssueRequest {
        @NotNull private Long workOrderId;
        private Long operationId;
        @NotBlank private String operatorCode;
        private String warehouseCode;
        private Instant postingDate;
        @Size(max = 1000) private String remark;
        @NotEmpty @Valid private List<MaterialIssueItemRequest> items = new ArrayList<>();

        public Long getWorkOrderId() { return workOrderId; }
        public void setWorkOrderId(Long value) { workOrderId = value; }
        public Long getOperationId() { return operationId; }
        public void setOperationId(Long value) { operationId = value; }
        public String getOperatorCode() { return operatorCode; }
        public void setOperatorCode(String value) { operatorCode = value; }
        public String getWarehouseCode() { return warehouseCode; }
        public void setWarehouseCode(String value) { warehouseCode = value; }
        public Instant getPostingDate() { return postingDate; }
        public void setPostingDate(Instant value) { postingDate = value; }
        public String getRemark() { return remark; }
        public void setRemark(String value) { remark = value; }
        public List<MaterialIssueItemRequest> getItems() { return items; }
        public void setItems(List<MaterialIssueItemRequest> value) { items = value; }
    }

    public static class MaterialIssueItemRequest {
        @NotBlank private String productCode;
        private String productName;
        private String batchNo;
        @NotNull @DecimalMin("0.000001") private BigDecimal quantity;
        @NotBlank private String unit;
        private String storageLocation;
        private String reservationNo;
        private String reservationItem;

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

    public static class WorkReportRequest {
        @NotNull private Long workOrderId;
        private Long operationId;
        @NotNull @DecimalMin("0.000001") private BigDecimal qualifiedQuantity;
        @NotNull @DecimalMin("0") private BigDecimal unqualifiedQuantity;
        @NotBlank private String operatorCode;
        private String shiftCode;
        private String equipmentCode;
        private Instant reportedAt;
        @Size(max = 1000) private String remark;

        public Long getWorkOrderId() { return workOrderId; }
        public void setWorkOrderId(Long value) { workOrderId = value; }
        public Long getOperationId() { return operationId; }
        public void setOperationId(Long value) { operationId = value; }
        public BigDecimal getQualifiedQuantity() { return qualifiedQuantity; }
        public void setQualifiedQuantity(BigDecimal value) { qualifiedQuantity = value; }
        public BigDecimal getUnqualifiedQuantity() { return unqualifiedQuantity; }
        public void setUnqualifiedQuantity(BigDecimal value) { unqualifiedQuantity = value; }
        public String getOperatorCode() { return operatorCode; }
        public void setOperatorCode(String value) { operatorCode = value; }
        public String getShiftCode() { return shiftCode; }
        public void setShiftCode(String value) { shiftCode = value; }
        public String getEquipmentCode() { return equipmentCode; }
        public void setEquipmentCode(String value) { equipmentCode = value; }
        public Instant getReportedAt() { return reportedAt; }
        public void setReportedAt(Instant value) { reportedAt = value; }
        public String getRemark() { return remark; }
        public void setRemark(String value) { remark = value; }
    }

    public static class InspectionRequest {
        @NotNull private Long workOrderId;
        private Long operationId;
        private Long workReportId;
        @NotBlank private String inspectionType;
        @NotNull @DecimalMin("0.000001") private BigDecimal sampleQuantity;
        @NotBlank private String requestedBy;
        private Instant requestedAt;
        @Size(max = 1000) private String remark;
        @Valid private List<InspectionItemRequest> items = new ArrayList<>();

        public Long getWorkOrderId() { return workOrderId; }
        public void setWorkOrderId(Long value) { workOrderId = value; }
        public Long getOperationId() { return operationId; }
        public void setOperationId(Long value) { operationId = value; }
        public Long getWorkReportId() { return workReportId; }
        public void setWorkReportId(Long value) { workReportId = value; }
        public String getInspectionType() { return inspectionType; }
        public void setInspectionType(String value) { inspectionType = value; }
        public BigDecimal getSampleQuantity() { return sampleQuantity; }
        public void setSampleQuantity(BigDecimal value) { sampleQuantity = value; }
        public String getRequestedBy() { return requestedBy; }
        public void setRequestedBy(String value) { requestedBy = value; }
        public Instant getRequestedAt() { return requestedAt; }
        public void setRequestedAt(Instant value) { requestedAt = value; }
        public String getRemark() { return remark; }
        public void setRemark(String value) { remark = value; }
        public List<InspectionItemRequest> getItems() { return items; }
        public void setItems(List<InspectionItemRequest> value) { items = value; }
    }

    public static class InspectionResultRequest {
        @NotBlank private String inspectorCode;
        @NotNull @DecimalMin("0") private BigDecimal qualifiedQuantity;
        @NotNull @DecimalMin("0") private BigDecimal unqualifiedQuantity;
        @NotBlank private String overallResult;
        private String defectCode;
        private String disposition;
        @Size(max = 1000) private String remark;
        @Valid private List<InspectionItemRequest> items = new ArrayList<>();

        public String getInspectorCode() { return inspectorCode; }
        public void setInspectorCode(String value) { inspectorCode = value; }
        public BigDecimal getQualifiedQuantity() { return qualifiedQuantity; }
        public void setQualifiedQuantity(BigDecimal value) { qualifiedQuantity = value; }
        public BigDecimal getUnqualifiedQuantity() { return unqualifiedQuantity; }
        public void setUnqualifiedQuantity(BigDecimal value) { unqualifiedQuantity = value; }
        public String getOverallResult() { return overallResult; }
        public void setOverallResult(String value) { overallResult = value; }
        public String getDefectCode() { return defectCode; }
        public void setDefectCode(String value) { defectCode = value; }
        public String getDisposition() { return disposition; }
        public void setDisposition(String value) { disposition = value; }
        public String getRemark() { return remark; }
        public void setRemark(String value) { remark = value; }
        public List<InspectionItemRequest> getItems() { return items; }
        public void setItems(List<InspectionItemRequest> value) { items = value; }
    }

    public static class InspectionItemRequest {
        @NotBlank private String itemCode;
        @NotBlank private String itemName;
        private String specification;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private String measuredValue;
        private String unit;
        private String result;
        private String remark;

        public String getItemCode() { return itemCode; }
        public void setItemCode(String value) { itemCode = value; }
        public String getItemName() { return itemName; }
        public void setItemName(String value) { itemName = value; }
        public String getSpecification() { return specification; }
        public void setSpecification(String value) { specification = value; }
        public BigDecimal getMinValue() { return minValue; }
        public void setMinValue(BigDecimal value) { minValue = value; }
        public BigDecimal getMaxValue() { return maxValue; }
        public void setMaxValue(BigDecimal value) { maxValue = value; }
        public String getMeasuredValue() { return measuredValue; }
        public void setMeasuredValue(String value) { measuredValue = value; }
        public String getUnit() { return unit; }
        public void setUnit(String value) { unit = value; }
        public String getResult() { return result; }
        public void setResult(String value) { result = value; }
        public String getRemark() { return remark; }
        public void setRemark(String value) { remark = value; }
    }

    public static class GoodsReceiptRequest {
        @NotNull private Long workOrderId;
        private Long workReportId;
        @NotNull private Long inspectionId;
        @NotNull @DecimalMin("0.000001") private BigDecimal quantity;
        @NotBlank private String unit;
        private String batchNo;
        @NotBlank private String warehouseCode;
        @NotBlank private String storageLocation;
        private String movementType;
        @NotBlank private String operatorCode;
        private Instant postingDate;
        @Size(max = 1000) private String remark;

        public Long getWorkOrderId() { return workOrderId; }
        public void setWorkOrderId(Long value) { workOrderId = value; }
        public Long getWorkReportId() { return workReportId; }
        public void setWorkReportId(Long value) { workReportId = value; }
        public Long getInspectionId() { return inspectionId; }
        public void setInspectionId(Long value) { inspectionId = value; }
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
        public String getOperatorCode() { return operatorCode; }
        public void setOperatorCode(String value) { operatorCode = value; }
        public Instant getPostingDate() { return postingDate; }
        public void setPostingDate(Instant value) { postingDate = value; }
        public String getRemark() { return remark; }
        public void setRemark(String value) { remark = value; }
    }
}
