package com.tns.mes.integration.sap;

import com.tns.mes.engineering.domain.Bom;
import com.tns.mes.engineering.domain.ProcessRoute;
import com.tns.mes.engineering.repo.BomRepository;
import com.tns.mes.engineering.repo.ProcessRouteRepository;
import com.tns.mes.engineering.repo.ProductRepository;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.repo.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mes.integration.sap.enabled=true",
        "mes.integration.sap.base-url=http://sap.test",
        "mes.integration.sap.operation-path="
})
class SapSyncServiceTests {
    @Autowired private SapSyncService service;
    @Autowired private RestTemplate externalRestTemplate;
    @Autowired private ProductRepository products;
    @Autowired private WorkOrderRepository workOrders;
    @Autowired private BomRepository boms;
    @Autowired private ProcessRouteRepository routes;

    @Test
    void synchronizesProductNameFromExpandedDescription() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(externalRestTemplate).build();
        server.expect(request -> {
            String query = URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8.name());
            assertTrue(query.contains("$select="));
            assertTrue(query.contains("to_Description"));
        }).andRespond(withSuccess(
                "{\"d\":{\"results\":[{\"Product\":\"7010000206AA\",\"ProductType\":\"Z10\",\"BaseUnit\":\"PCS\","
                        + "\"to_Description\":{\"results\":[{\"Language\":\"ZH\",\"ProductDescription\":\"HODPE袋透明\"}]}}]}}",
                MediaType.APPLICATION_JSON));

        SapSyncService.SyncResult result = service.syncProduct("7010000206AA");
        server.verify();

        assertEquals(1, result.getCreated());
        com.tns.mes.engineering.domain.Product product = products.findByCode("7010000206AA")
                .orElseThrow(AssertionError::new);
        assertEquals("HODPE袋透明", product.getName());
        assertEquals("HODPE袋透明", product.getNameZh());
        assertNull(product.getNameEn());
        assertEquals("HODPE袋透明", product.getProductDescription());
    }

    @Test
    void continuesWithSkipWhenSapOmitsNextLink() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(externalRestTemplate).build();
        server.expect(request -> {
            String query = URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8.name());
            assertTrue(query.contains("$top=2"));
            assertTrue(!query.contains("$skip="));
        }).andRespond(withSuccess(
                "{\"d\":{\"results\":[{\"Product\":\"PAGE-1\",\"ProductDescription\":\"First\"},{\"Product\":\"PAGE-2\",\"ProductDescription\":\"Second\"}]}}",
                MediaType.APPLICATION_JSON));
        server.expect(request -> {
            String query = URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8.name());
            assertTrue(query.contains("$top=2"));
            assertTrue(query.contains("$skip=2"));
        }).andRespond(withSuccess(
                "{\"d\":{\"results\":[{\"Product\":\"PAGE-3\",\"ProductDescription\":\"Third\"}]}}",
                MediaType.APPLICATION_JSON));
        Map<String, Object> query = new HashMap<>();
        query.put("$top", 2);
        query.put("$orderby", "Product");

        SapSyncService.SyncResult result = service.syncProducts(null, query);
        server.verify();

        assertEquals(3, result.getReceived());
        assertEquals(3, result.getCreated());
    }

    @Test
    void synchronizesSingleWorkOrderWithComponentsAndRoute() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(externalRestTemplate).build();
        server.expect(requestTo(containsString("/API_PRODUCT_SRV/A_Product"))).andRespond(withSuccess(
                "{\"d\":{\"results\":[{\"Product\":\"FG-100\",\"ProductDescription\":\"Finished product\",\"BaseUnit\":\"PCS\"}]}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/API_PRODUCTION_ORDER_2_SRV/A_ProductionOrder_2"))).andRespond(withSuccess(
                "{\"d\":{\"results\":[{\"ManufacturingOrder\":\"MO-100\",\"Material\":\"FG-100\",\"MfgOrderPlannedTotalQty\":\"12\",\"MfgOrderIsReleased\":true," +
                        "\"to_ProductionOrderComponent\":{\"results\":[{\"Material\":\"COMP-1\",\"RequiredQuantity\":\"2\",\"BaseUnit\":\"PCS\"}]}," +
                        "\"to_ProductionOrderOperation\":{\"results\":[{\"Operation\":\"0010\",\"OperationText\":\"Assembly\",\"StandardDuration\":\"30\"}]}}]}}", MediaType.APPLICATION_JSON));
        service.syncProduct("FG-100");
        SapSyncService.SyncResult result = service.syncWorkOrder("MO-100");
        server.verify();

        assertEquals(1, result.getCreated());
        WorkOrder order = workOrders.findByOrderNo("MO-100").orElseThrow(AssertionError::new);
        assertNotNull(order.getBom());
        assertNotNull(order.getRoute());
        Bom bom = boms.findWithItemsById(order.getBom().getId()).orElseThrow(AssertionError::new);
        ProcessRoute route = routes.findWithOperationsById(order.getRoute().getId()).orElseThrow(AssertionError::new);
        assertEquals(1, bom.getItems().size());
        assertEquals(1, route.getOperations().size());
        assertEquals("COMP-1", products.findById(bom.getItems().get(0).getComponentProductId()).orElseThrow(AssertionError::new).getCode());
    }
}
