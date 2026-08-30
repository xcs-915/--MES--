package com.tns.mes.production.web;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.service.WorkOrderService;
import com.tns.mes.engineering.repo.ProductRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/work-orders")
@Validated
public class WorkOrderController {
    private final WorkOrderService service;
    private final ProductRepository products;
    public WorkOrderController(WorkOrderService service, ProductRepository products){this.service=service;this.products=products;}
    @GetMapping @PreAuthorize("hasAuthority('WORK_ORDER_READ')")
    public ApiResponse<PageResponse<WorkOrderView>> page(@RequestParam(required=false) String keyword,
                                                         @RequestParam(required=false) String status,
                                                         @RequestParam(required=false) String orderType,
                                                         @RequestParam(required=false) String plant,
                                                         @RequestParam(defaultValue="0") int page,
                                                         @RequestParam(defaultValue="20") int size,
                                                         HttpServletRequest request){PageResponse<WorkOrder> values=service.page(keyword,status,orderType,plant,page,size);return ApiResponse.ok(new PageResponse<>(values.getItems().stream().map(WorkOrderView::new).collect(Collectors.toList()),values.getTotal(),values.getPage(),values.getSize(),values.getTotalPages()),id(request));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('WORK_ORDER_READ')")
    public ApiResponse<WorkOrderView> get(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(new WorkOrderView(service.get(id),products),id(request));}
    @PostMapping @PreAuthorize("hasAuthority('WORK_ORDER_WRITE')")
    public ApiResponse<WorkOrderView> create(@Valid @RequestBody WorkOrderService.WorkOrderRequest body,HttpServletRequest request){return ApiResponse.ok(new WorkOrderView(service.create(body)),id(request));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('WORK_ORDER_WRITE')")
    public ApiResponse<WorkOrderView> update(@PathVariable Long id,@Valid @RequestBody WorkOrderService.WorkOrderRequest body,HttpServletRequest request){return ApiResponse.ok(new WorkOrderView(service.update(id,body)),id(request));}
    @PostMapping("/{id}/release") @PreAuthorize("hasAuthority('WORK_ORDER_WRITE')")
    public ApiResponse<WorkOrderView> release(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(new WorkOrderView(service.release(id)),id(request));}
    @PostMapping("/{id}/start") @PreAuthorize("hasAuthority('WORK_ORDER_WRITE')")
    public ApiResponse<WorkOrderView> start(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(new WorkOrderView(service.start(id)),id(request));}
    @PostMapping("/{id}/complete") @PreAuthorize("hasAuthority('WORK_ORDER_WRITE')")
    public ApiResponse<WorkOrderView> complete(@PathVariable Long id,@Valid @RequestBody CompleteRequest body,HttpServletRequest request){return ApiResponse.ok(new WorkOrderView(service.complete(id,body.getQuantity())),id(request));}
    @PostMapping("/{id}/cancel") @PreAuthorize("hasAuthority('WORK_ORDER_WRITE')")
    public ApiResponse<WorkOrderView> cancel(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(new WorkOrderView(service.cancel(id)),id(request));}
    private String id(HttpServletRequest request){Object value=request.getAttribute("requestId");return value==null?null:value.toString();}
    public static class CompleteRequest { @javax.validation.constraints.NotNull @DecimalMin("0.000001") private BigDecimal quantity; public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal value){quantity=value;} }
}
