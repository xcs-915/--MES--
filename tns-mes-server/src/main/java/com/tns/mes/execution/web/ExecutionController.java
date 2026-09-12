package com.tns.mes.execution.web;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.execution.service.GoodsReceiptService;
import com.tns.mes.execution.service.InspectionService;
import com.tns.mes.execution.service.MaterialIssueService;
import com.tns.mes.execution.service.WorkReportService;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/execution")
public class ExecutionController {
    private final MaterialIssueService materialIssues;
    private final WorkReportService workReports;
    private final InspectionService inspections;
    private final GoodsReceiptService goodsReceipts;

    public ExecutionController(MaterialIssueService materialIssues, WorkReportService workReports,
                               InspectionService inspections, GoodsReceiptService goodsReceipts) {
        this.materialIssues = materialIssues;
        this.workReports = workReports;
        this.inspections = inspections;
        this.goodsReceipts = goodsReceipts;
    }

    @GetMapping("/material-issues")
    @PreAuthorize("hasAuthority('EXECUTION_READ')")
    public ApiResponse<PageResponse<ExecutionViews.MaterialIssueView>> materialIssues(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.ok(materialIssues.page(keyword, status, page, size), requestId(request));
    }

    @PostMapping("/material-issues")
    @PreAuthorize("hasAuthority('EXECUTION_WRITE')")
    public ApiResponse<ExecutionViews.MaterialIssueView> createMaterialIssue(
            @Valid @RequestBody ExecutionRequests.MaterialIssueRequest body, HttpServletRequest request) {
        return ApiResponse.ok(materialIssues.create(body), requestId(request));
    }

    @PostMapping("/material-issues/{id}/post")
    @PreAuthorize("hasAuthority('EXECUTION_POST')")
    public ApiResponse<ExecutionViews.MaterialIssueView> postMaterialIssue(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(materialIssues.post(id), requestId(request));
    }

    @GetMapping("/work-reports")
    @PreAuthorize("hasAuthority('EXECUTION_READ')")
    public ApiResponse<PageResponse<ExecutionViews.WorkReportView>> workReports(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.ok(workReports.page(keyword, status, page, size), requestId(request));
    }

    @PostMapping("/work-reports")
    @PreAuthorize("hasAuthority('EXECUTION_WRITE')")
    public ApiResponse<ExecutionViews.WorkReportView> createWorkReport(
            @Valid @RequestBody ExecutionRequests.WorkReportRequest body, HttpServletRequest request) {
        return ApiResponse.ok(workReports.create(body), requestId(request));
    }

    @PostMapping("/work-reports/{id}/post")
    @PreAuthorize("hasAuthority('EXECUTION_POST')")
    public ApiResponse<ExecutionViews.WorkReportView> postWorkReport(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(workReports.post(id), requestId(request));
    }

    @GetMapping("/inspections")
    @PreAuthorize("hasAuthority('QUALITY_READ')")
    public ApiResponse<PageResponse<ExecutionViews.InspectionView>> inspections(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String type,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        return ApiResponse.ok(inspections.page(keyword, type, status, page, size), requestId(request));
    }

    @PostMapping("/inspections/requests")
    @PreAuthorize("hasAuthority('QUALITY_WRITE')")
    public ApiResponse<ExecutionViews.InspectionView> requestInspection(
            @Valid @RequestBody ExecutionRequests.InspectionRequest body, HttpServletRequest request) {
        return ApiResponse.ok(inspections.request(body), requestId(request));
    }

    @PutMapping("/inspections/{id}/result")
    @PreAuthorize("hasAuthority('QUALITY_WRITE')")
    public ApiResponse<ExecutionViews.InspectionView> completeInspection(
            @PathVariable Long id, @Valid @RequestBody ExecutionRequests.InspectionResultRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(inspections.complete(id, body), requestId(request));
    }

    @GetMapping("/goods-receipts")
    @PreAuthorize("hasAuthority('EXECUTION_READ')")
    public ApiResponse<PageResponse<ExecutionViews.GoodsReceiptView>> goodsReceipts(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.ok(goodsReceipts.page(keyword, status, page, size), requestId(request));
    }

    @PostMapping("/goods-receipts")
    @PreAuthorize("hasAuthority('EXECUTION_WRITE')")
    public ApiResponse<ExecutionViews.GoodsReceiptView> createGoodsReceipt(
            @Valid @RequestBody ExecutionRequests.GoodsReceiptRequest body, HttpServletRequest request) {
        return ApiResponse.ok(goodsReceipts.create(body), requestId(request));
    }

    @PostMapping("/goods-receipts/{id}/post")
    @PreAuthorize("hasAuthority('EXECUTION_POST')")
    public ApiResponse<ExecutionViews.GoodsReceiptView> postGoodsReceipt(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(goodsReceipts.post(id), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute("requestId");
        return value == null ? null : value.toString();
    }
}
