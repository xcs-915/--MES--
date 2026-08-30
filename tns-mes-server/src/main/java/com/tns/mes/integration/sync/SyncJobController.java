package com.tns.mes.integration.sync;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.integration.sap.SapSyncService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations/sync-jobs")
@Validated
public class SyncJobController {
    private final SyncJobService service;
    public SyncJobController(SyncJobService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ApiResponse<List<SyncJobService.JobView>> list(@RequestParam(required=false) String keyword,
                                                          @RequestParam(required=false) String status,
                                                          HttpServletRequest request) {
        return ApiResponse.ok(service.list(keyword, status), id(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEGRATION_READ')")
    public ApiResponse<SyncJobService.JobView> detail(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(service.detail(id), id(request));
    }

    @PostMapping("/{id}/run")
    @PreAuthorize("hasAnyAuthority('SYNC_JOB_RUN','INTEGRATION_WRITE')")
    public ApiResponse<SapSyncService.SyncResult> run(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(service.runById(id, "MANUAL"), id(request));
    }

    @PutMapping("/{id}/enabled")
    @PreAuthorize("hasAnyAuthority('SYNC_JOB_TOGGLE','INTEGRATION_WRITE')")
    public ApiResponse<SyncJobService.JobView> enabled(@PathVariable Long id, @RequestParam boolean value,
                                                       HttpServletRequest request) {
        return ApiResponse.ok(service.setEnabled(id, value), id(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYNC_JOB_EDIT','INTEGRATION_WRITE')")
    public ApiResponse<SyncJobService.JobView> update(@PathVariable Long id,
                                                      @Valid @RequestBody SyncJobService.JobUpdateRequest body,
                                                      HttpServletRequest request) {
        return ApiResponse.ok(service.update(id, body), id(request));
    }

    private String id(HttpServletRequest request) {
        Object value = request.getAttribute("requestId");
        return value == null ? null : value.toString();
    }
}
