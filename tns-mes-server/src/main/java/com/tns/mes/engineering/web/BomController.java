package com.tns.mes.engineering.web;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.engineering.domain.Bom;
import com.tns.mes.engineering.service.BomService;
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
@RequestMapping("/api/v1/boms")
@Validated
public class BomController {
    private final BomService service;
    public BomController(BomService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAuthority('ENGINEERING_READ')")
    public ApiResponse<PageResponse<EngineeringViewMapper.BomView>> page(@RequestParam(required=false) Long productId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size, HttpServletRequest request) {
        PageResponse<Bom> values=service.page(productId,page,size); return ApiResponse.ok(new PageResponse<>(values.getItems().stream().map(EngineeringViewMapper::bom).collect(Collectors.toList()), values.getTotal(), values.getPage(), values.getSize(), values.getTotalPages()), id(request));
    }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('ENGINEERING_READ')")
    public ApiResponse<EngineeringViewMapper.BomView> get(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(EngineeringViewMapper.bom(service.get(id)),id(request));}
    @PostMapping @PreAuthorize("hasAuthority('ENGINEERING_WRITE')")
    public ApiResponse<EngineeringViewMapper.BomView> create(@Valid @RequestBody BomService.BomRequest body,HttpServletRequest request){return ApiResponse.ok(EngineeringViewMapper.bom(service.create(body)),id(request));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('ENGINEERING_WRITE')")
    public ApiResponse<EngineeringViewMapper.BomView> update(@PathVariable Long id,@Valid @RequestBody BomService.BomRequest body,HttpServletRequest request){return ApiResponse.ok(EngineeringViewMapper.bom(service.update(id,body)),id(request));}
    @PostMapping("/{id}/publish") @PreAuthorize("hasAuthority('ENGINEERING_WRITE')")
    public ApiResponse<EngineeringViewMapper.BomView> publish(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(EngineeringViewMapper.bom(service.publish(id)),id(request));}
    private String id(HttpServletRequest request){Object value=request.getAttribute("requestId");return value==null?null:value.toString();}
}

