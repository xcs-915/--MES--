package com.tns.mes.engineering.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.engineering.domain.EsopDocument;
import com.tns.mes.engineering.domain.EsopReviewLog;
import com.tns.mes.engineering.repo.EsopDocumentRepository;
import com.tns.mes.engineering.repo.EsopReviewLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EsopDocumentService {
    private final EsopDocumentRepository repository;
    private final EsopReviewLogRepository reviewLogRepository;
    private final ObjectMapper objectMapper;

    public EsopDocumentService(EsopDocumentRepository repository, EsopReviewLogRepository reviewLogRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.reviewLogRepository = reviewLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<EsopDocument> page(String keyword, String status, String productCode, int page, int size) {
        Specification<EsopDocument> spec = (root, query, cb) -> {
            List<Predicate> values = new ArrayList<>();
            if (hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                values.add(cb.or(cb.like(cb.lower(root.get("documentNo")), like),
                        cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("productCode")), like),
                        cb.like(cb.lower(root.get("productName")), like)));
            }
            if (hasText(status)) values.add(cb.equal(root.get("status"), status.trim().toUpperCase(Locale.ROOT)));
            if (hasText(productCode)) values.add(cb.equal(root.get("productCode"), productCode.trim()));
            return cb.and(values.toArray(new Predicate[0]));
        };
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
        Page<EsopDocument> result = repository.findAll(spec, pageable);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public EsopDocument get(Long id) {
        return repository.findById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
    }

    @Transactional
    @Auditable(action = "CREATE", resource = "ESOP_DOCUMENT")
    public EsopDocument create(EsopRequest request) {
        validate(request);
        String documentNo = clean(request.getDocumentNo());
        String versionCode = clean(request.getVersionCode());
        if (repository.existsByDocumentNoAndVersionCode(documentNo, versionCode)) throw new BizException(4091, "error.duplicate");
        EsopDocument value = new EsopDocument();
        apply(value, request);
        value.setStatus("DRAFT");
        return repository.save(value);
    }

    @Transactional
    @Auditable(action = "UPDATE", resource = "ESOP_DOCUMENT")
    public EsopDocument update(Long id, EsopRequest request) {
        EsopDocument value = get(id);
        ensureDraft(value);
        validate(request);
        String documentNo = clean(request.getDocumentNo());
        String versionCode = clean(request.getVersionCode());
        if ((!documentNo.equals(value.getDocumentNo()) || !versionCode.equals(value.getVersionCode()))
                && repository.existsByDocumentNoAndVersionCode(documentNo, versionCode)) throw new BizException(4091, "error.duplicate");
        apply(value, request);
        return repository.save(value);
    }

    @Transactional
    @Auditable(action = "SUBMIT", resource = "ESOP_DOCUMENT")
    public EsopDocument submit(Long id) {
        EsopDocument value = get(id);
        ensureDraft(value);
        validateContent(readContent(value));
        value.setStatus("SUBMITTED");
        value.setSubmittedAt(Instant.now());
        value.setReviewComment(null);
        return repository.save(value);
    }

    @Transactional
    @Auditable(action = "APPROVE", resource = "ESOP_DOCUMENT")
    public EsopDocument approve(Long id, String comment) {
        EsopDocument value = get(id);
        ensureStatus(value, "SUBMITTED");
        value.setStatus("APPROVED");
        value.setReviewedBy(currentUser());
        value.setReviewedAt(Instant.now());
        value.setApprovedAt(Instant.now());
        value.setApprovedBy(currentUser());
        value.setReviewComment(cleanNullable(comment));
        logReview(value.getId(), "APPROVE", comment);
        return repository.save(value);
    }

    @Transactional
    @Auditable(action = "REJECT", resource = "ESOP_DOCUMENT")
    public EsopDocument reject(Long id, String comment) {
        EsopDocument value = get(id);
        ensureStatus(value, "SUBMITTED");
        value.setStatus("DRAFT");
        value.setReviewedBy(currentUser());
        value.setReviewedAt(Instant.now());
        value.setReviewComment(cleanNullable(comment));
        logReview(value.getId(), "REJECT", comment);
        return repository.save(value);
    }

    @Transactional
    @Auditable(action = "PUBLISH", resource = "ESOP_DOCUMENT")
    public EsopDocument publish(Long id) {
        EsopDocument value = get(id);
        ensureStatus(value, "APPROVED");
        for (EsopDocument active : repository.findByDocumentNoAndStatus(value.getDocumentNo(), "PUBLISHED")) {
            if (!active.getId().equals(value.getId())) active.setStatus("OBSOLETE");
        }
        value.setStatus("PUBLISHED");
        value.setPublishedAt(Instant.now());
        value.setPublishedBy(currentUser());
        if (value.getEffectiveDate() == null) value.setEffectiveDate(LocalDate.now());
        return repository.save(value);
    }

    @Transactional
    @Auditable(action = "REVISE", resource = "ESOP_DOCUMENT")
    public EsopDocument revise(Long id, RevisionRequest request) {
        EsopDocument source = get(id);
        if (!"PUBLISHED".equals(source.getStatus()) && !"OBSOLETE".equals(source.getStatus())) throw new BizException(4092, "error.invalid-state");
        String versionCode = clean(request.getVersionCode());
        if (!hasText(versionCode) || repository.existsByDocumentNoAndVersionCode(source.getDocumentNo(), versionCode)) throw new BizException(4091, "error.duplicate");
        EsopDocument value = new EsopDocument();
        value.setDocumentNo(source.getDocumentNo()); value.setVersionCode(versionCode); value.setTitle(source.getTitle());
        value.setProductCode(source.getProductCode()); value.setProductName(source.getProductName()); value.setDocumentType(source.getDocumentType());
        value.setStatus("DRAFT"); value.setChangeSummary(request.getChangeSummary()); value.setPreparedBy(currentUser());
        value.setReviewedBy(null); value.setApprovedBy(null); value.setEffectiveDate(null); value.setContentJson(source.getContentJson());
        value.setSubmittedAt(null); value.setReviewedAt(null); value.setApprovedAt(null); value.setReviewComment(null);
        return repository.save(value);
    }

    @Transactional
    @Auditable(action = "DELETE", resource = "ESOP_DOCUMENT")
    public void delete(Long id) {
        EsopDocument value = get(id);
        ensureDraft(value);
        repository.delete(value);
    }

    public JsonNode readContent(EsopDocument value) {
        try { return objectMapper.readTree(value.getContentJson()); }
        catch (JsonProcessingException ex) { throw new BizException(5001, "error.internal"); }
    }

    private void apply(EsopDocument value, EsopRequest request) {
        value.setDocumentNo(clean(request.getDocumentNo())); value.setVersionCode(clean(request.getVersionCode()));
        value.setTitle(clean(request.getTitle())); value.setProductCode(cleanNullable(request.getProductCode()));
        value.setProductName(cleanNullable(request.getProductName())); value.setDocumentType(clean(request.getDocumentType()).toUpperCase(Locale.ROOT));
        value.setEffectiveDate(request.getEffectiveDate()); value.setChangeSummary(cleanNullable(request.getChangeSummary()));
        value.setPreparedBy(cleanNullable(request.getPreparedBy())); value.setReviewedBy(cleanNullable(request.getReviewedBy()));
        value.setApprovedBy(cleanNullable(request.getApprovedBy()));
        try { value.setContentJson(objectMapper.writeValueAsString(request.getContent())); }
        catch (JsonProcessingException ex) { throw new BizException(4003, "error.validation"); }
    }

    private void validate(EsopRequest request) {
        if (!hasText(request.getDocumentNo()) || !hasText(request.getVersionCode()) || !hasText(request.getTitle())
                || !hasText(request.getDocumentType())) throw new BizException(4003, "error.validation");
        String type = request.getDocumentType().trim().toUpperCase(Locale.ROOT);
        if (!type.equals("ASSEMBLY") && !type.equals("WEAVING") && !type.equals("GENERAL")) throw new BizException(4003, "error.validation");
        validateContent(request.getContent());
    }

    private void validateContent(JsonNode content) {
        if (content == null || !content.isObject() || !content.path("pages").isArray() || content.path("pages").size() == 0)
            throw new BizException(4003, "error.validation");
    }

    private void ensureDraft(EsopDocument value) {
        if (!"DRAFT".equals(value.getStatus())) throw new BizException(4092, "error.invalid-state");
    }
    private void ensureStatus(EsopDocument value, String status) {
        if (!status.equals(value.getStatus())) throw new BizException(4092, "error.invalid-state");
    }
    private void logReview(Long documentId, String action, String comment) {
        EsopReviewLog entry = new EsopReviewLog();
        entry.setDocumentId(documentId); entry.setAction(action); entry.setActor(currentUser());
        entry.setComment(cleanNullable(comment));
        reviewLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<EsopReviewLog> reviewLogs(Long documentId) {
        return reviewLogRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    private String currentUser() { Authentication auth = SecurityContextHolder.getContext().getAuthentication(); return auth == null ? "system" : auth.getName(); }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String cleanNullable(String value) { String result = clean(value); return result.isEmpty() ? null : result; }

    public static class EsopRequest {
        private String documentNo; private String versionCode; private String title; private String productCode; private String productName;
        private String documentType; private LocalDate effectiveDate; private String changeSummary; private String preparedBy; private String reviewedBy; private String approvedBy; private JsonNode content;
        public String getDocumentNo() { return documentNo; } public void setDocumentNo(String v) { documentNo=v; }
        public String getVersionCode() { return versionCode; } public void setVersionCode(String v) { versionCode=v; }
        public String getTitle() { return title; } public void setTitle(String v) { title=v; }
        public String getProductCode() { return productCode; } public void setProductCode(String v) { productCode=v; }
        public String getProductName() { return productName; } public void setProductName(String v) { productName=v; }
        public String getDocumentType() { return documentType; } public void setDocumentType(String v) { documentType=v; }
        public LocalDate getEffectiveDate() { return effectiveDate; } public void setEffectiveDate(LocalDate v) { effectiveDate=v; }
        public String getChangeSummary() { return changeSummary; } public void setChangeSummary(String v) { changeSummary=v; }
        public String getPreparedBy() { return preparedBy; } public void setPreparedBy(String v) { preparedBy=v; }
        public String getReviewedBy() { return reviewedBy; } public void setReviewedBy(String v) { reviewedBy=v; }
        public String getApprovedBy() { return approvedBy; } public void setApprovedBy(String v) { approvedBy=v; }
        public JsonNode getContent() { return content; } public void setContent(JsonNode v) { content=v; }
    }

    public static class RevisionRequest {
        private String versionCode; private String changeSummary;
        public String getVersionCode() { return versionCode; } public void setVersionCode(String value) { versionCode=value; }
        public String getChangeSummary() { return changeSummary; } public void setChangeSummary(String value) { changeSummary=value; }
    }
}
