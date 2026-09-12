package com.tns.mes.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.engineering.domain.Product;
import com.tns.mes.engineering.repo.ProductRepository;
import com.tns.mes.execution.repo.GoodsReceiptRepository;
import com.tns.mes.execution.repo.MaterialIssueRepository;
import com.tns.mes.execution.repo.QualityInspectionRepository;
import com.tns.mes.execution.repo.WorkReportRepository;
import com.tns.mes.execution.service.GoodsReceiptService;
import com.tns.mes.execution.service.InspectionService;
import com.tns.mes.execution.service.MaterialIssueService;
import com.tns.mes.execution.service.WorkReportService;
import com.tns.mes.execution.web.ExecutionRequests.GoodsReceiptRequest;
import com.tns.mes.execution.web.ExecutionRequests.InspectionRequest;
import com.tns.mes.execution.web.ExecutionRequests.InspectionResultRequest;
import com.tns.mes.execution.web.ExecutionRequests.MaterialIssueItemRequest;
import com.tns.mes.execution.web.ExecutionRequests.MaterialIssueRequest;
import com.tns.mes.execution.web.ExecutionRequests.WorkReportRequest;
import com.tns.mes.execution.web.ExecutionViews.GoodsReceiptView;
import com.tns.mes.execution.web.ExecutionViews.InspectionView;
import com.tns.mes.execution.web.ExecutionViews.MaterialIssueView;
import com.tns.mes.execution.web.ExecutionViews.WorkReportView;
import com.tns.mes.integration.middleware.MiddlewareIntegrationService;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.repo.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ExecutionFlowTests {
    @Autowired private MaterialIssueService materialIssueService;
    @Autowired private WorkReportService workReportService;
    @Autowired private InspectionService inspectionService;
    @Autowired private GoodsReceiptService goodsReceiptService;
    @Autowired private ProductRepository products;
    @Autowired private WorkOrderRepository workOrders;
    @Autowired private MaterialIssueRepository materialIssues;
    @Autowired private WorkReportRepository workReports;
    @Autowired private QualityInspectionRepository inspections;
    @Autowired private GoodsReceiptRepository receipts;
    @Autowired private ObjectMapper mapper;
    @MockBean private MiddlewareIntegrationService middleware;

    private WorkOrder order;

    @BeforeEach
    void setUp() throws Exception {
        receipts.deleteAll();
        inspections.deleteAll();
        workReports.deleteAll();
        materialIssues.deleteAll();
        workOrders.deleteAll();
        products.deleteAll();

        Product product = new Product();
        product.setCode("TEST-FG");
        product.setName("Test product");
        product.setNameZh("测试产品");
        product.setProductType("FINISHED");
        product.setUnit("PCS");
        product.setStatus("ACTIVE");
        product.setTraceable(true);
        product = products.save(product);

        order = new WorkOrder();
        order.setOrderNo("WO-EXEC-001");
        order.setProduct(product);
        order.setQuantity(new BigDecimal("100"));
        order.setCompletedQuantity(BigDecimal.ZERO);
        order.setStatus("IN_PROGRESS");
        order.setOrderType("TK01");
        order.setProductionPlant("TK10");
        order.setProductionUnit("PCS");
        order = workOrders.save(order);

        when(middleware.postFeedback(any())).thenReturn(mapper.readTree("{\"success\":true,\"code\":200,\"result\":{\"docId\":\"D001\"}}"));
        when(middleware.postMoFeeding(any())).thenReturn(mapper.readTree("{\"success\":true,\"code\":200,\"result\":{\"confirmationGroup\":\"CONF001\",\"id\":\"BG001\"}}"));
        when(middleware.postFinishEntity(any())).thenReturn(mapper.readTree("{\"success\":true,\"code\":200,\"result\":{\"docId\":\"RC001\"}}"));
    }

    @Test
    void postsMaterialIssueWithExternalDocument() {
        MaterialIssueItemRequest item = new MaterialIssueItemRequest();
        item.setProductCode("RM-001"); item.setProductName("Raw material"); item.setQuantity(new BigDecimal("2")); item.setUnit("PCS"); item.setStorageLocation("J127");
        MaterialIssueRequest request = new MaterialIssueRequest();
        request.setWorkOrderId(order.getId()); request.setOperatorCode("OP01"); request.setWarehouseCode("TK10"); request.setItems(Collections.singletonList(item));

        MaterialIssueView created = materialIssueService.create(request);
        MaterialIssueView posted = materialIssueService.post(created.getId());

        assertEquals("POSTED", posted.getStatus());
        assertEquals("D001", posted.getExternalDocId());
    }

    @Test
    void blocksReportingUntilFirstInspectionPassesThenUpdatesQuantity() {
        InspectionView first = inspectionService.request(inspectionRequest("FIRST", BigDecimal.ONE));
        WorkReportView draft = workReportService.create(reportRequest(new BigDecimal("12")));

        assertThrows(BizException.class, () -> workReportService.post(draft.getId()));

        inspectionService.complete(first.getId(), inspectionResult(BigDecimal.ONE));
        WorkReportView posted = workReportService.post(draft.getId());

        assertEquals("POSTED", posted.getStatus());
        assertEquals("CONF001", posted.getConfirmationGroup());
        assertEquals(new BigDecimal("12.000000"), workOrders.findById(order.getId()).orElseThrow(AssertionError::new).getCompletedQuantity());
    }

    @Test
    void requiresPassedCompletionInspectionForGoodsReceipt() {
        WorkReportView report = workReportService.create(reportRequest(new BigDecimal("20")));
        report = workReportService.post(report.getId());
        InspectionView completion = inspectionService.request(inspectionRequest("COMPLETION", new BigDecimal("20")));
        GoodsReceiptRequest request = receiptRequest(completion.getId(), report.getId(), new BigDecimal("20"));

        assertThrows(BizException.class, () -> goodsReceiptService.create(request));

        inspectionService.complete(completion.getId(), inspectionResult(new BigDecimal("20")));
        GoodsReceiptView receipt = goodsReceiptService.create(request);
        GoodsReceiptView posted = goodsReceiptService.post(receipt.getId());

        assertEquals("POSTED", posted.getStatus());
        assertEquals("RC001", posted.getExternalDocId());
    }

    @Test
    void blocksCumulativeReceiptsBeyondInspectionQuantity() {
        WorkReportView report = workReportService.create(reportRequest(new BigDecimal("40")));
        report = workReportService.post(report.getId());
        InspectionView completion = inspectionService.request(inspectionRequest("COMPLETION", new BigDecimal("20")));
        inspectionService.complete(completion.getId(), inspectionResult(new BigDecimal("20")));

        GoodsReceiptView first = goodsReceiptService.create(receiptRequest(completion.getId(), report.getId(), new BigDecimal("15")));
        goodsReceiptService.post(first.getId());
        GoodsReceiptView second = goodsReceiptService.create(receiptRequest(completion.getId(), report.getId(), new BigDecimal("10")));

        assertThrows(BizException.class, () -> goodsReceiptService.post(second.getId()));
    }

    private WorkReportRequest reportRequest(BigDecimal quantity) {
        WorkReportRequest request = new WorkReportRequest();
        request.setWorkOrderId(order.getId()); request.setQualifiedQuantity(quantity); request.setUnqualifiedQuantity(BigDecimal.ZERO); request.setOperatorCode("OP01");
        return request;
    }

    private InspectionRequest inspectionRequest(String type, BigDecimal quantity) {
        InspectionRequest request = new InspectionRequest();
        request.setWorkOrderId(order.getId()); request.setInspectionType(type); request.setSampleQuantity(quantity); request.setRequestedBy("QC01");
        return request;
    }

    private InspectionResultRequest inspectionResult(BigDecimal quantity) {
        InspectionResultRequest request = new InspectionResultRequest();
        request.setInspectorCode("QC02"); request.setQualifiedQuantity(quantity); request.setUnqualifiedQuantity(BigDecimal.ZERO); request.setOverallResult("PASS");
        return request;
    }

    private GoodsReceiptRequest receiptRequest(Long inspectionId, Long reportId, BigDecimal quantity) {
        GoodsReceiptRequest request = new GoodsReceiptRequest();
        request.setWorkOrderId(order.getId()); request.setWorkReportId(reportId); request.setInspectionId(inspectionId); request.setQuantity(quantity); request.setUnit("PCS");
        request.setWarehouseCode("TK10"); request.setStorageLocation("J127"); request.setMovementType("1101P"); request.setOperatorCode("WH01");
        return request;
    }
}
