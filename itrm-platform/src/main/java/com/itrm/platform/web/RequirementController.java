package com.itrm.platform.web;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.common.PageResponse;
import com.itrm.platform.domain.ApprovalRecord;
import com.itrm.platform.domain.ChangeLog;
import com.itrm.platform.domain.Requirement;
import com.itrm.platform.service.RequirementService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    // ==================== 查询 ====================

    @GetMapping
    public ApiResponse<PageResponse<Requirement>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sourceDept,
            @RequestParam(required = false) String assignee,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(requirementService.page(keyword, status, urgency, category, sourceDept, assignee, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Requirement> get(@PathVariable Long id) {
        return ApiResponse.ok(requirementService.get(id));
    }

    // ==================== CRUD ====================

    @PostMapping
    @PreAuthorize("hasAuthority('requirement:create')")
    public ApiResponse<Requirement> create(@RequestBody Requirement request) {
        return ApiResponse.ok(requirementService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('requirement:edit','requirement:change')")
    public ApiResponse<Requirement> update(@PathVariable Long id, @RequestBody Requirement request) {
        return ApiResponse.ok(requirementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('requirement:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        requirementService.delete(id);
        return ApiResponse.ok();
    }

    // ==================== 状态机流转 ====================

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('requirement:submit')")
    public ApiResponse<Requirement> submit(@PathVariable Long id) {
        return ApiResponse.ok(requirementService.submit(id));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('requirement:receive')")
    public ApiResponse<Requirement> receive(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String reviewOwner = (String) body.get("reviewOwner");
        Long templateId = body.get("templateId") != null ? Long.valueOf(body.get("templateId").toString()) : null;
        return ApiResponse.ok(requirementService.receive(id, reviewOwner, templateId));
    }

    @PostMapping("/{id}/plan")
    @PreAuthorize("hasAuthority('requirement:plan')")
    public ApiResponse<Requirement> plan(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String devOwner = (String) body.get("devOwner");
        LocalDate plannedStart = body.get("plannedStartDate") != null ? LocalDate.parse(body.get("plannedStartDate").toString()) : null;
        LocalDate plannedEnd = body.get("plannedEndDate") != null ? LocalDate.parse(body.get("plannedEndDate").toString()) : null;
        LocalDate devPlannedEnd = body.get("devPlannedEnd") != null ? LocalDate.parse(body.get("devPlannedEnd").toString()) : null;
        BigDecimal estimatedHours = body.get("estimatedHours") != null ? new BigDecimal(body.get("estimatedHours").toString()) : BigDecimal.ZERO;
        return ApiResponse.ok(requirementService.plan(id, devOwner, plannedStart, plannedEnd, devPlannedEnd, estimatedHours));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('requirement:review')")
    public ApiResponse<Requirement> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String action = body.get("action");
        String comment = body.get("comment");
        return ApiResponse.ok(requirementService.approveNode(id, action, comment));
    }

    @PostMapping("/{id}/start-testing")
    @PreAuthorize("hasAuthority('test:transition')")
    public ApiResponse<Requirement> startTesting(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String tester = body.get("tester");
        return ApiResponse.ok(requirementService.startTesting(id, tester));
    }

    @PostMapping("/{id}/finish-testing")
    @PreAuthorize("hasAuthority('test:transition')")
    public ApiResponse<Requirement> finishTesting(@PathVariable Long id) {
        return ApiResponse.ok(requirementService.finishTesting(id));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('requirement:release')")
    public ApiResponse<Requirement> release(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean success = body.get("success") != null ? Boolean.valueOf(body.get("success").toString()) : false;
        return ApiResponse.ok(requirementService.release(id, success));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('requirement:review')")
    public ApiResponse<Requirement> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String comment = body.get("comment");
        return ApiResponse.ok(requirementService.reject(id, comment));
    }

    // ==================== 进度 ====================

    @PutMapping("/{id}/progress")
    @PreAuthorize("hasAnyAuthority('requirement:edit','task:transition')")
    public ApiResponse<Requirement> updateProgress(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer progress = body.get("progress");
        return ApiResponse.ok(requirementService.updateProgress(id, progress));
    }

    // ==================== 审批记录 & 变更记录 ====================

    @GetMapping("/{id}/approval-records")
    public ApiResponse<List<ApprovalRecord>> approvalRecords(@PathVariable Long id) {
        return ApiResponse.ok(requirementService.approvalRecords(id));
    }

    @GetMapping("/{id}/change-logs")
    public ApiResponse<List<ChangeLog>> changeLogs(@PathVariable Long id) {
        return ApiResponse.ok(requirementService.changeLogs(id));
    }

    // ==================== 统计 ====================

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(requirementService.stats());
    }
}
