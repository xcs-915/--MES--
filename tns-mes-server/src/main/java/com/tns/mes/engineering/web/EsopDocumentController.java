package com.tns.mes.engineering.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.engineering.domain.EsopDocument;
import com.tns.mes.engineering.domain.EsopReviewLog;
import com.tns.mes.engineering.service.EsopDocumentService;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/esops")
public class EsopDocumentController {
    private final EsopDocumentService service;
    public EsopDocumentController(EsopDocumentService service) { this.service = service; }

    @GetMapping @PreAuthorize("hasAuthority('ESOP_READ')")
    public ApiResponse<PageResponse<EsopView>> page(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status, @RequestParam(required = false) String productCode,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        PageResponse<EsopDocument> values = service.page(keyword, status, productCode, page, size);
        return ApiResponse.ok(new PageResponse<>(values.getItems().stream().map(v -> view(v, false)).collect(Collectors.toList()),
                values.getTotal(), values.getPage(), values.getSize(), values.getTotalPages()), id(request));
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('ESOP_READ')")
    public ApiResponse<EsopView> get(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.ok(view(service.get(id), true), id(request)); }
    @PostMapping @PreAuthorize("hasAuthority('ESOP_WRITE')")
    public ApiResponse<EsopView> create(@RequestBody EsopDocumentService.EsopRequest body, HttpServletRequest request) { return ApiResponse.ok(view(service.create(body), true), id(request)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('ESOP_WRITE')")
    public ApiResponse<EsopView> update(@PathVariable Long id, @RequestBody EsopDocumentService.EsopRequest body, HttpServletRequest request) { return ApiResponse.ok(view(service.update(id, body), true), id(request)); }
    @PostMapping("/{id}/submit") @PreAuthorize("hasAuthority('ESOP_WRITE')")
    public ApiResponse<EsopView> submit(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.ok(view(service.submit(id), true), id(request)); }
    @PostMapping("/{id}/approve") @PreAuthorize("hasAuthority('ESOP_REVIEW')")
    public ApiResponse<EsopView> approve(@PathVariable Long id, @RequestBody(required = false) CommentRequest body, HttpServletRequest request) { return ApiResponse.ok(view(service.approve(id, body == null ? null : body.getComment()), true), id(request)); }
    @PostMapping("/{id}/reject") @PreAuthorize("hasAuthority('ESOP_REVIEW')")
    public ApiResponse<EsopView> reject(@PathVariable Long id, @RequestBody(required = false) CommentRequest body, HttpServletRequest request) { return ApiResponse.ok(view(service.reject(id, body == null ? null : body.getComment()), true), id(request)); }
    @PostMapping("/{id}/publish") @PreAuthorize("hasAuthority('ESOP_PUBLISH')")
    public ApiResponse<EsopView> publish(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.ok(view(service.publish(id), true), id(request)); }
    @GetMapping("/{id}/reviews") @PreAuthorize("hasAuthority('ESOP_READ')")
    public ApiResponse<java.util.List<EsopReviewLog>> reviews(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.ok(service.reviewLogs(id), id(request)); }
    @PostMapping("/{id}/revise") @PreAuthorize("hasAuthority('ESOP_WRITE')")
    public ApiResponse<EsopView> revise(@PathVariable Long id, @RequestBody EsopDocumentService.RevisionRequest body, HttpServletRequest request) { return ApiResponse.ok(view(service.revise(id, body), true), id(request)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('ESOP_WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) { service.delete(id); return ApiResponse.ok(null, id(request)); }

    private EsopView view(EsopDocument value, boolean includeContent) { return new EsopView(value, includeContent ? service.readContent(value) : null); }
    private String id(HttpServletRequest request) { Object value=request.getAttribute("requestId"); return value==null?null:value.toString(); }

    public static class EsopView {
        private final Long id; private final String documentNo; private final String versionCode; private final String title;
        private final String productCode; private final String productName; private final String documentType; private final String status;
        private final LocalDate effectiveDate; private final String changeSummary; private final String preparedBy; private final String reviewedBy;
        private final String approvedBy; private final Instant publishedAt; private final String publishedBy; private final Instant createdAt;
        private final Instant updatedAt; private final Instant submittedAt; private final Instant reviewedAt; private final String reviewComment; private final JsonNode content;
        EsopView(EsopDocument v, JsonNode content) { id=v.getId(); documentNo=v.getDocumentNo(); versionCode=v.getVersionCode(); title=v.getTitle(); productCode=v.getProductCode(); productName=v.getProductName(); documentType=v.getDocumentType(); status=v.getStatus(); effectiveDate=v.getEffectiveDate(); changeSummary=v.getChangeSummary(); preparedBy=v.getPreparedBy(); reviewedBy=v.getReviewedBy(); approvedBy=v.getApprovedBy(); publishedAt=v.getPublishedAt(); publishedBy=v.getPublishedBy(); createdAt=v.getCreatedAt(); updatedAt=v.getUpdatedAt(); submittedAt=v.getSubmittedAt(); reviewedAt=v.getReviewedAt(); reviewComment=v.getReviewComment(); this.content=content; }
        public Long getId(){return id;} public String getDocumentNo(){return documentNo;} public String getVersionCode(){return versionCode;} public String getTitle(){return title;} public String getProductCode(){return productCode;} public String getProductName(){return productName;} public String getDocumentType(){return documentType;} public String getStatus(){return status;} public LocalDate getEffectiveDate(){return effectiveDate;} public String getChangeSummary(){return changeSummary;} public String getPreparedBy(){return preparedBy;} public String getReviewedBy(){return reviewedBy;} public String getApprovedBy(){return approvedBy;} public Instant getPublishedAt(){return publishedAt;} public String getPublishedBy(){return publishedBy;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public Instant getSubmittedAt(){return submittedAt;} public Instant getReviewedAt(){return reviewedAt;} public String getReviewComment(){return reviewComment;} public JsonNode getContent(){return content;}
    }

    public static class CommentRequest {
        private String comment;
        public String getComment() { return comment; }
        public void setComment(String value) { comment = value; }
    }
}
