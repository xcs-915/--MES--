package com.tns.mes.engineering.web;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.engineering.domain.Product;
import com.tns.mes.engineering.service.ProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/products")
@Validated
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAuthority('ENGINEERING_READ')")
    public ApiResponse<PageResponse<EngineeringViewMapper.ProductView>> page(@RequestParam(required=false) String keyword,
                                                                              @RequestParam(required=false) String productType,
                                                                              @RequestParam(required=false) String status,
                                                                              @RequestParam(defaultValue="0") int page,
                                                                              @RequestParam(defaultValue="20") int size,
                                                                              HttpServletRequest request) {
        PageResponse<Product> values = service.page(keyword, productType, status, page, size);
        PageResponse<EngineeringViewMapper.ProductView> result = new PageResponse<>(values.getItems().stream().map(EngineeringViewMapper::product).collect(Collectors.toList()), values.getTotal(), values.getPage(), values.getSize(), values.getTotalPages());
        return ApiResponse.ok(result, id(request));
    }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('ENGINEERING_READ')")
    public ApiResponse<EngineeringViewMapper.ProductView> get(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.ok(EngineeringViewMapper.product(service.get(id)), id(request)); }
    @PostMapping @PreAuthorize("hasAuthority('ENGINEERING_WRITE')")
    public ApiResponse<EngineeringViewMapper.ProductView> create(@Valid @RequestBody ProductService.ProductRequest body, HttpServletRequest request) { return ApiResponse.ok(EngineeringViewMapper.product(service.create(body)), id(request)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('ENGINEERING_WRITE')")
    public ApiResponse<EngineeringViewMapper.ProductView> update(@PathVariable Long id, @Valid @RequestBody ProductService.ProductRequest body, HttpServletRequest request) { return ApiResponse.ok(EngineeringViewMapper.product(service.update(id, body)), id(request)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('ENGINEERING_WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) { service.delete(id); return ApiResponse.ok(null, id(request)); }
    private String id(HttpServletRequest request) { Object value=request.getAttribute("requestId"); return value == null ? null : value.toString(); }
}
