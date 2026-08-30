package com.tns.mes.engineering.web;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.engineering.domain.ProcessRoute;
import com.tns.mes.engineering.service.ProcessRouteService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/process-routes")
@Validated
public class ProcessRouteController {
    private final ProcessRouteService service;
    public ProcessRouteController(ProcessRouteService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('ENGINEERING_READ')")
    public ApiResponse<PageResponse<EngineeringViewMapper.RouteView>> page(@RequestParam(required=false) Long productId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request){PageResponse<ProcessRoute> values=service.page(productId,page,size);return ApiResponse.ok(new PageResponse<>(values.getItems().stream().map(EngineeringViewMapper::route).collect(Collectors.toList()),values.getTotal(),values.getPage(),values.getSize(),values.getTotalPages()),id(request));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('ENGINEERING_READ')")
    public ApiResponse<EngineeringViewMapper.RouteView> get(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(EngineeringViewMapper.route(service.get(id)),id(request));}
    @PostMapping @PreAuthorize("hasAuthority('ENGINEERING_WRITE')")
    public ApiResponse<EngineeringViewMapper.RouteView> create(@Valid @RequestBody ProcessRouteService.ProcessRouteRequest body,HttpServletRequest request){return ApiResponse.ok(EngineeringViewMapper.route(service.create(body)),id(request));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('ENGINEERING_WRITE')")
    public ApiResponse<EngineeringViewMapper.RouteView> update(@PathVariable Long id,@Valid @RequestBody ProcessRouteService.ProcessRouteRequest body,HttpServletRequest request){return ApiResponse.ok(EngineeringViewMapper.route(service.update(id,body)),id(request));}
    @PostMapping("/{id}/publish") @PreAuthorize("hasAuthority('ENGINEERING_WRITE')")
    public ApiResponse<EngineeringViewMapper.RouteView> publish(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(EngineeringViewMapper.route(service.publish(id)),id(request));}
    private String id(HttpServletRequest request){Object value=request.getAttribute("requestId");return value==null?null:value.toString();}
}

