package com.tns.mes.execution.web;

import com.tns.mes.execution.domain.GoodsReceipt;
import com.tns.mes.execution.domain.MaterialIssue;
import com.tns.mes.execution.domain.MaterialIssueItem;
import com.tns.mes.execution.domain.QualityInspection;
import com.tns.mes.execution.domain.QualityInspectionItem;
import com.tns.mes.execution.domain.WorkReport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public final class ExecutionViews {
    private ExecutionViews() { }

    public static class MaterialIssueView {
        private final Long id;
        private final String issueNo;
        private final Long workOrderId;
        private final String orderNo;
        private final Long operationId;
        private final String operationCode;
        private final String status;
        private final Instant postingDate;
        private final String warehouseCode;
        private final String operatorCode;
        private final String externalDocId;
        private final String externalMessage;
        private final Instant postedAt;
        private final String remark;
        private final Instant createdAt;
        private final List<MaterialIssueItemView> items;

        public MaterialIssueView(MaterialIssue value) {
            id=value.getId(); issueNo=value.getIssueNo(); workOrderId=value.getWorkOrder().getId(); orderNo=value.getWorkOrder().getOrderNo();
            operationId=value.getOperation()==null?null:value.getOperation().getId(); operationCode=operationCode(value.getOperation()); status=value.getStatus();
            postingDate=value.getPostingDate(); warehouseCode=value.getWarehouseCode(); operatorCode=value.getOperatorCode(); externalDocId=value.getExternalDocId();
            externalMessage=value.getExternalMessage(); postedAt=value.getPostedAt(); remark=value.getRemark(); createdAt=value.getCreatedAt();
            items=value.getItems().stream().map(MaterialIssueItemView::new).collect(Collectors.toList());
        }
        public Long getId(){return id;} public String getIssueNo(){return issueNo;} public Long getWorkOrderId(){return workOrderId;} public String getOrderNo(){return orderNo;}
        public Long getOperationId(){return operationId;} public String getOperationCode(){return operationCode;} public String getStatus(){return status;} public Instant getPostingDate(){return postingDate;}
        public String getWarehouseCode(){return warehouseCode;} public String getOperatorCode(){return operatorCode;} public String getExternalDocId(){return externalDocId;}
        public String getExternalMessage(){return externalMessage;} public Instant getPostedAt(){return postedAt;} public String getRemark(){return remark;} public Instant getCreatedAt(){return createdAt;}
        public List<MaterialIssueItemView> getItems(){return items;}
    }

    public static class MaterialIssueItemView {
        private final Long id; private final Integer lineNo; private final String productCode; private final String productName; private final String batchNo;
        private final BigDecimal quantity; private final String unit; private final String storageLocation; private final String reservationNo; private final String reservationItem;
        MaterialIssueItemView(MaterialIssueItem value) { id=value.getId(); lineNo=value.getLineNo(); productCode=value.getProductCode(); productName=value.getProductName(); batchNo=value.getBatchNo(); quantity=value.getQuantity(); unit=value.getUnit(); storageLocation=value.getStorageLocation(); reservationNo=value.getReservationNo(); reservationItem=value.getReservationItem(); }
        public Long getId(){return id;} public Integer getLineNo(){return lineNo;} public String getProductCode(){return productCode;} public String getProductName(){return productName;}
        public String getBatchNo(){return batchNo;} public BigDecimal getQuantity(){return quantity;} public String getUnit(){return unit;} public String getStorageLocation(){return storageLocation;}
        public String getReservationNo(){return reservationNo;} public String getReservationItem(){return reservationItem;}
    }

    public static class WorkReportView {
        private final Long id; private final String reportNo; private final Long workOrderId; private final String orderNo; private final String productCode; private final String productName;
        private final Long operationId; private final String operationCode; private final String operationName; private final String status; private final BigDecimal qualifiedQuantity;
        private final BigDecimal unqualifiedQuantity; private final String operatorCode; private final String shiftCode; private final String equipmentCode; private final Instant reportedAt;
        private final String confirmationGroup; private final String externalDocId; private final String externalMessage; private final Instant postedAt; private final String remark; private final Instant createdAt;
        public WorkReportView(WorkReport value) {
            id=value.getId(); reportNo=value.getReportNo(); workOrderId=value.getWorkOrder().getId(); orderNo=value.getWorkOrder().getOrderNo();
            productCode=value.getWorkOrder().getProduct().getCode(); productName=value.getWorkOrder().getProduct().getNameZh(); operationId=value.getOperation()==null?null:value.getOperation().getId();
            operationCode=operationCode(value.getOperation()); operationName=value.getOperation()==null?null:value.getOperation().getOperationName(); status=value.getStatus(); qualifiedQuantity=value.getQualifiedQuantity();
            unqualifiedQuantity=value.getUnqualifiedQuantity(); operatorCode=value.getOperatorCode(); shiftCode=value.getShiftCode(); equipmentCode=value.getEquipmentCode(); reportedAt=value.getReportedAt();
            confirmationGroup=value.getConfirmationGroup(); externalDocId=value.getExternalDocId(); externalMessage=value.getExternalMessage(); postedAt=value.getPostedAt(); remark=value.getRemark(); createdAt=value.getCreatedAt();
        }
        public Long getId(){return id;} public String getReportNo(){return reportNo;} public Long getWorkOrderId(){return workOrderId;} public String getOrderNo(){return orderNo;}
        public String getProductCode(){return productCode;} public String getProductName(){return productName;} public Long getOperationId(){return operationId;} public String getOperationCode(){return operationCode;}
        public String getOperationName(){return operationName;} public String getStatus(){return status;} public BigDecimal getQualifiedQuantity(){return qualifiedQuantity;} public BigDecimal getUnqualifiedQuantity(){return unqualifiedQuantity;}
        public String getOperatorCode(){return operatorCode;} public String getShiftCode(){return shiftCode;} public String getEquipmentCode(){return equipmentCode;} public Instant getReportedAt(){return reportedAt;}
        public String getConfirmationGroup(){return confirmationGroup;} public String getExternalDocId(){return externalDocId;} public String getExternalMessage(){return externalMessage;} public Instant getPostedAt(){return postedAt;}
        public String getRemark(){return remark;} public Instant getCreatedAt(){return createdAt;}
    }

    public static class InspectionView {
        private final Long id; private final String inspectionNo; private final String inspectionType; private final Long workOrderId; private final String orderNo;
        private final String productCode; private final String productName; private final Long operationId; private final String operationCode; private final Long workReportId;
        private final String reportNo; private final String status; private final BigDecimal sampleQuantity; private final BigDecimal qualifiedQuantity; private final BigDecimal unqualifiedQuantity;
        private final String requestedBy; private final Instant requestedAt; private final String inspectorCode; private final Instant inspectedAt; private final String defectCode;
        private final String disposition; private final String remark; private final Instant createdAt; private final List<InspectionItemView> items;
        public InspectionView(QualityInspection value) {
            id=value.getId(); inspectionNo=value.getInspectionNo(); inspectionType=value.getInspectionType(); workOrderId=value.getWorkOrder().getId(); orderNo=value.getWorkOrder().getOrderNo();
            productCode=value.getWorkOrder().getProduct().getCode(); productName=value.getWorkOrder().getProduct().getNameZh(); operationId=value.getOperation()==null?null:value.getOperation().getId();
            operationCode=operationCode(value.getOperation()); workReportId=value.getWorkReport()==null?null:value.getWorkReport().getId(); reportNo=value.getWorkReport()==null?null:value.getWorkReport().getReportNo();
            status=value.getStatus(); sampleQuantity=value.getSampleQuantity(); qualifiedQuantity=value.getQualifiedQuantity(); unqualifiedQuantity=value.getUnqualifiedQuantity(); requestedBy=value.getRequestedBy();
            requestedAt=value.getRequestedAt(); inspectorCode=value.getInspectorCode(); inspectedAt=value.getInspectedAt(); defectCode=value.getDefectCode(); disposition=value.getDisposition();
            remark=value.getRemark(); createdAt=value.getCreatedAt(); items=value.getItems().stream().map(InspectionItemView::new).collect(Collectors.toList());
        }
        public Long getId(){return id;} public String getInspectionNo(){return inspectionNo;} public String getInspectionType(){return inspectionType;} public Long getWorkOrderId(){return workOrderId;}
        public String getOrderNo(){return orderNo;} public String getProductCode(){return productCode;} public String getProductName(){return productName;} public Long getOperationId(){return operationId;}
        public String getOperationCode(){return operationCode;} public Long getWorkReportId(){return workReportId;} public String getReportNo(){return reportNo;} public String getStatus(){return status;}
        public BigDecimal getSampleQuantity(){return sampleQuantity;} public BigDecimal getQualifiedQuantity(){return qualifiedQuantity;} public BigDecimal getUnqualifiedQuantity(){return unqualifiedQuantity;}
        public String getRequestedBy(){return requestedBy;} public Instant getRequestedAt(){return requestedAt;} public String getInspectorCode(){return inspectorCode;} public Instant getInspectedAt(){return inspectedAt;}
        public String getDefectCode(){return defectCode;} public String getDisposition(){return disposition;} public String getRemark(){return remark;} public Instant getCreatedAt(){return createdAt;}
        public List<InspectionItemView> getItems(){return items;}
    }

    public static class InspectionItemView {
        private final Long id; private final Integer lineNo; private final String itemCode; private final String itemName; private final String specification;
        private final BigDecimal minValue; private final BigDecimal maxValue; private final String measuredValue; private final String unit; private final String result; private final String remark;
        InspectionItemView(QualityInspectionItem value) { id=value.getId(); lineNo=value.getLineNo(); itemCode=value.getItemCode(); itemName=value.getItemName(); specification=value.getSpecification(); minValue=value.getMinValue(); maxValue=value.getMaxValue(); measuredValue=value.getMeasuredValue(); unit=value.getUnit(); result=value.getResult(); remark=value.getRemark(); }
        public Long getId(){return id;} public Integer getLineNo(){return lineNo;} public String getItemCode(){return itemCode;} public String getItemName(){return itemName;}
        public String getSpecification(){return specification;} public BigDecimal getMinValue(){return minValue;} public BigDecimal getMaxValue(){return maxValue;} public String getMeasuredValue(){return measuredValue;}
        public String getUnit(){return unit;} public String getResult(){return result;} public String getRemark(){return remark;}
    }

    public static class GoodsReceiptView {
        private final Long id; private final String receiptNo; private final Long workOrderId; private final String orderNo; private final String productCode; private final String productName;
        private final Long workReportId; private final String reportNo; private final Long inspectionId; private final String inspectionNo; private final BigDecimal quantity; private final String unit;
        private final String batchNo; private final String warehouseCode; private final String storageLocation; private final String movementType; private final String status; private final String operatorCode;
        private final Instant postingDate; private final String externalDocId; private final String externalMessage; private final Instant postedAt; private final String remark; private final Instant createdAt;
        public GoodsReceiptView(GoodsReceipt value) {
            id=value.getId(); receiptNo=value.getReceiptNo(); workOrderId=value.getWorkOrder().getId(); orderNo=value.getWorkOrder().getOrderNo(); productCode=value.getWorkOrder().getProduct().getCode();
            productName=value.getWorkOrder().getProduct().getNameZh(); workReportId=value.getWorkReport()==null?null:value.getWorkReport().getId(); reportNo=value.getWorkReport()==null?null:value.getWorkReport().getReportNo();
            inspectionId=value.getInspection().getId(); inspectionNo=value.getInspection().getInspectionNo(); quantity=value.getQuantity(); unit=value.getUnit(); batchNo=value.getBatchNo(); warehouseCode=value.getWarehouseCode();
            storageLocation=value.getStorageLocation(); movementType=value.getMovementType(); status=value.getStatus(); operatorCode=value.getOperatorCode(); postingDate=value.getPostingDate(); externalDocId=value.getExternalDocId();
            externalMessage=value.getExternalMessage(); postedAt=value.getPostedAt(); remark=value.getRemark(); createdAt=value.getCreatedAt();
        }
        public Long getId(){return id;} public String getReceiptNo(){return receiptNo;} public Long getWorkOrderId(){return workOrderId;} public String getOrderNo(){return orderNo;}
        public String getProductCode(){return productCode;} public String getProductName(){return productName;} public Long getWorkReportId(){return workReportId;} public String getReportNo(){return reportNo;}
        public Long getInspectionId(){return inspectionId;} public String getInspectionNo(){return inspectionNo;} public BigDecimal getQuantity(){return quantity;} public String getUnit(){return unit;}
        public String getBatchNo(){return batchNo;} public String getWarehouseCode(){return warehouseCode;} public String getStorageLocation(){return storageLocation;} public String getMovementType(){return movementType;}
        public String getStatus(){return status;} public String getOperatorCode(){return operatorCode;} public Instant getPostingDate(){return postingDate;} public String getExternalDocId(){return externalDocId;}
        public String getExternalMessage(){return externalMessage;} public Instant getPostedAt(){return postedAt;} public String getRemark(){return remark;} public Instant getCreatedAt(){return createdAt;}
    }

    private static String operationCode(com.tns.mes.production.domain.WorkOrderOperation operation) {
        if (operation == null) return null;
        return operation.getOperationCode() == null ? String.valueOf(operation.getSequenceNo()) : operation.getOperationCode();
    }
}
