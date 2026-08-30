package com.tns.mes.quality.web;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.quality.domain.Batch;
import com.tns.mes.quality.service.BatchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/quality/batches")
public class BatchController {
    private final BatchService service;

    public BatchController(BatchService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('QUALITY_READ')")
    public ApiResponse<PageResponse<Batch>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String batchStatus,
                                                  @RequestParam(required = false) String plant,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  HttpServletRequest request) {
        return ApiResponse.ok(service.page(keyword, batchStatus, plant, page, size), id(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('QUALITY_READ')")
    public ApiResponse<Batch> get(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(service.get(id), id(request));
    }

    private String id(HttpServletRequest request) { Object value = request.getAttribute("requestId"); return value == null ? null : value.toString(); }
}
