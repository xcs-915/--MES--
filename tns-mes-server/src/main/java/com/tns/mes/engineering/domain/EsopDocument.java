package com.tns.mes.engineering.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "eng_esop_document")
public class EsopDocument extends AuditedEntity {
    @Column(name = "document_no", nullable = false, length = 64)
    private String documentNo;
    @Column(name = "version_code", nullable = false, length = 32)
    private String versionCode;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(name = "product_code", length = 64)
    private String productCode;
    @Column(name = "product_name", length = 200)
    private String productName;
    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    @Column(name = "change_summary", length = 1000)
    private String changeSummary;
    @Column(name = "prepared_by", length = 64)
    private String preparedBy;
    @Column(name = "reviewed_by", length = 64)
    private String reviewedBy;
    @Column(name = "approved_by", length = 64)
    private String approvedBy;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "published_by", length = 64)
    private String publishedBy;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "approved_at")
    private Instant approvedAt;
    @Column(name = "review_comment", length = 1000)
    private String reviewComment;
    @Column(name = "content_json", nullable = false, columnDefinition = "nvarchar(max)")
    private String contentJson;

    public String getDocumentNo() { return documentNo; }
    public void setDocumentNo(String value) { documentNo = value; }
    public String getVersionCode() { return versionCode; }
    public void setVersionCode(String value) { versionCode = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String value) { productCode = value; }
    public String getProductName() { return productName; }
    public void setProductName(String value) { productName = value; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String value) { documentType = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate value) { effectiveDate = value; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String value) { changeSummary = value; }
    public String getPreparedBy() { return preparedBy; }
    public void setPreparedBy(String value) { preparedBy = value; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String value) { reviewedBy = value; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String value) { approvedBy = value; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant value) { publishedAt = value; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String value) { publishedBy = value; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant value) { submittedAt = value; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant value) { reviewedAt = value; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant value) { approvedAt = value; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String value) { reviewComment = value; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String value) { contentJson = value; }
}
