package com.tns.mes.production.service;

import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.integration.outbox.OutboxService;
import com.tns.mes.engineering.domain.Bom;
import com.tns.mes.engineering.domain.ProcessOperation;
import com.tns.mes.engineering.domain.ProcessRoute;
import com.tns.mes.engineering.domain.Product;
import com.tns.mes.engineering.repo.BomRepository;
import com.tns.mes.engineering.repo.ProcessRouteRepository;
import com.tns.mes.engineering.repo.ProductRepository;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.domain.WorkOrderOperation;
import com.tns.mes.production.repo.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class WorkOrderService {
    private final WorkOrderRepository repository;
    private final ProductRepository products;
    private final BomRepository boms;
    private final ProcessRouteRepository routes;
    private final OutboxService outbox;

    public WorkOrderService(WorkOrderRepository repository, ProductRepository products, BomRepository boms, ProcessRouteRepository routes, OutboxService outbox) {
        this.repository = repository; this.products = products; this.boms = boms; this.routes = routes; this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrder> page(String keyword, String status, String orderType, String plant, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "priority").and(Sort.by("plannedStart")));
        Specification<WorkOrder> spec = Specification.where(null);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String term = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("orderNo")), term),
                    cb.like(cb.lower(root.join("product").get("code")), term),
                    cb.like(cb.lower(root.join("product").get("nameZh")), term)));
        }
        if (status != null && !status.trim().isEmpty())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase()));
        if (orderType != null && !orderType.trim().isEmpty())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("orderType"), orderType.trim()));
        if (plant != null && !plant.trim().isEmpty())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("productionPlant"), plant.trim()));
        Page<WorkOrder> result = repository.findAll(spec, pageable);
        return PageResponse.from(result);
    }

    public PageResponse<WorkOrder> page(String status, int page, int size) {
        return page(null, status, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public WorkOrder get(Long id) {
        WorkOrder order = repository.findWithRelationsById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
        if (order.getBom() != null) order.getBom().getItems().size();
        if (order.getRoute() != null) order.getRoute().getOperations().size();
        return order;
    }

    @Transactional
    @Auditable(action = "CREATE", resource = "WORK_ORDER")
    public WorkOrder create(WorkOrderRequest request) {
        Product product = products.findById(request.getProductId()).orElseThrow(() -> new BizException(4042, "error.not-found"));
        Bom bom = request.getBomId() == null ? null : boms.findWithItemsById(request.getBomId()).orElseThrow(() -> new BizException(4042, "error.not-found"));
        ProcessRoute route = request.getRouteId() == null ? null : routes.findWithOperationsById(request.getRouteId()).orElseThrow(() -> new BizException(4042, "error.not-found"));
        if (bom != null && !bom.getProductId().equals(product.getId())) throw new BizException(4003, "error.validation");
        if (route != null && !route.getProductId().equals(product.getId())) throw new BizException(4003, "error.validation");
        WorkOrder order = new WorkOrder();
        order.setOrderNo(request.getOrderNo() == null || request.getOrderNo().trim().isEmpty() ? generateOrderNo() : request.getOrderNo().trim());
        if (repository.existsByOrderNo(order.getOrderNo())) throw new BizException(4091, "error.duplicate");
        apply(order, request, product, bom, route);
        WorkOrder saved = repository.save(order);
        outbox.enqueue("WORK_ORDER", String.valueOf(saved.getId()), "WORK_ORDER_CREATED", eventPayload(saved));
        return saved;
    }

    @Transactional
    @Auditable(action = "UPDATE", resource = "WORK_ORDER")
    public WorkOrder update(Long id, WorkOrderRequest request) {
        WorkOrder order = get(id);
        if (!"DRAFT".equals(order.getStatus())) throw new BizException(4092, "error.invalid-state");
        Product product = products.findById(request.getProductId()).orElseThrow(() -> new BizException(4042, "error.not-found"));
        Bom bom = request.getBomId() == null ? null : boms.findWithItemsById(request.getBomId()).orElseThrow(() -> new BizException(4042, "error.not-found"));
        ProcessRoute route = request.getRouteId() == null ? null : routes.findWithOperationsById(request.getRouteId()).orElseThrow(() -> new BizException(4042, "error.not-found"));
        if (bom != null && !bom.getProductId().equals(product.getId()) || route != null && !route.getProductId().equals(product.getId())) throw new BizException(4003, "error.validation");
        apply(order, request, product, bom, route);
        WorkOrder saved = repository.save(order);
        outbox.enqueue("WORK_ORDER", String.valueOf(saved.getId()), "WORK_ORDER_UPDATED", eventPayload(saved));
        return saved;
    }

    @Transactional
    @Auditable(action = "RELEASE", resource = "WORK_ORDER")
    public WorkOrder release(Long id) { return transition(id, "DRAFT", "RELEASED"); }
    @Transactional
    @Auditable(action = "START", resource = "WORK_ORDER")
    public WorkOrder start(Long id) { return transition(id, "RELEASED", "IN_PROGRESS"); }
    @Transactional
    @Auditable(action = "CANCEL", resource = "WORK_ORDER")
    public WorkOrder cancel(Long id) {
        WorkOrder order = get(id);
        if (!"DRAFT".equals(order.getStatus()) && !"RELEASED".equals(order.getStatus())) throw new BizException(4092, "error.invalid-state");
        order.setStatus("CANCELLED"); WorkOrder saved = repository.save(order); outbox.enqueue("WORK_ORDER", String.valueOf(saved.getId()), "WORK_ORDER_CANCELLED", eventPayload(saved)); return saved;
    }
    @Transactional
    @Auditable(action = "COMPLETE", resource = "WORK_ORDER")
    public WorkOrder complete(Long id, BigDecimal quantity) {
        WorkOrder order = get(id);
        if (!"IN_PROGRESS".equals(order.getStatus())) throw new BizException(4092, "error.invalid-state");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0 || order.getCompletedQuantity().add(quantity).compareTo(order.getQuantity()) > 0) throw new BizException(4003, "error.validation");
        order.setCompletedQuantity(order.getCompletedQuantity().add(quantity));
        if (order.getCompletedQuantity().compareTo(order.getQuantity()) >= 0) { order.setStatus("COMPLETED"); for (WorkOrderOperation op : order.getOperations()) op.setStatus("COMPLETED"); }
        WorkOrder saved = repository.save(order); outbox.enqueue("WORK_ORDER", String.valueOf(saved.getId()), "WORK_ORDER_PROGRESS", eventPayload(saved)); return saved;
    }

    private WorkOrder transition(Long id, String expected, String target) {
        WorkOrder order = get(id); if (!expected.equals(order.getStatus())) throw new BizException(4092, "error.invalid-state"); order.setStatus(target); WorkOrder saved = repository.save(order); outbox.enqueue("WORK_ORDER", String.valueOf(saved.getId()), "WORK_ORDER_" + target, eventPayload(saved)); return saved;
    }

    private void apply(WorkOrder order, WorkOrderRequest request, Product product, Bom bom, ProcessRoute route) {
        order.setProduct(product); order.setBom(bom); order.setRoute(route); order.setFactoryId(request.getFactoryId()); order.setWorkshopId(request.getWorkshopId()); order.setQuantity(request.getQuantity()); order.setPriority(request.getPriority() == null ? 50 : request.getPriority()); order.setPlannedStart(request.getPlannedStart()); order.setPlannedEnd(request.getPlannedEnd()); order.setSource(request.getSource() == null ? "MANUAL" : request.getSource()); order.setRemark(request.getRemark());
        if (order.getCompletedQuantity() == null) order.setCompletedQuantity(BigDecimal.ZERO);
        order.getOperations().clear();
        if (route != null) for (ProcessOperation processOperation : route.getOperations()) { WorkOrderOperation operation = new WorkOrderOperation(); operation.setOperation(processOperation); operation.setSequenceNo(processOperation.getSequenceNo()); operation.setPlannedQuantity(request.getQuantity()); order.addOperation(operation); }
    }

    private String generateOrderNo() { return "WO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }

    private java.util.Map<String, Object> eventPayload(WorkOrder order) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderNo", order.getOrderNo());
        payload.put("status", order.getStatus());
        payload.put("productId", order.getProduct() == null ? null : order.getProduct().getId());
        payload.put("quantity", order.getQuantity());
        payload.put("completedQuantity", order.getCompletedQuantity());
        return payload;
    }

    public static class WorkOrderRequest {
        private String orderNo;
        @javax.validation.constraints.NotNull private Long productId;
        private Long bomId; private Long routeId; private Long factoryId; private Long workshopId;
        @javax.validation.constraints.NotNull @javax.validation.constraints.DecimalMin("0.000001") private BigDecimal quantity;
        @javax.validation.constraints.Min(0) private Integer priority; private LocalDateTime plannedStart; private LocalDateTime plannedEnd; private String source; private String remark;
        public String getOrderNo(){return orderNo;} public void setOrderNo(String value){orderNo=value;} public Long getProductId(){return productId;} public void setProductId(Long value){productId=value;} public Long getBomId(){return bomId;} public void setBomId(Long value){bomId=value;} public Long getRouteId(){return routeId;} public void setRouteId(Long value){routeId=value;} public Long getFactoryId(){return factoryId;} public void setFactoryId(Long value){factoryId=value;} public Long getWorkshopId(){return workshopId;} public void setWorkshopId(Long value){workshopId=value;} public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal value){quantity=value;} public Integer getPriority(){return priority;} public void setPriority(Integer value){priority=value;} public LocalDateTime getPlannedStart(){return plannedStart;} public void setPlannedStart(LocalDateTime value){plannedStart=value;} public LocalDateTime getPlannedEnd(){return plannedEnd;} public void setPlannedEnd(LocalDateTime value){plannedEnd=value;} public String getSource(){return source;} public void setSource(String value){source=value;} public String getRemark(){return remark;} public void setRemark(String value){remark=value;}
    }
}
