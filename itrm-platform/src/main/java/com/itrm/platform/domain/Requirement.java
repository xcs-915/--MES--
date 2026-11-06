package com.itrm.platform.domain;

import com.itrm.platform.common.BaseEntity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "itrm_requirement")
public class Requirement extends BaseEntity {

    @Column(name = "req_no", length = 32, unique = true, nullable = false)
    private String reqNo;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "urgency", length = 20)
    private String urgency = "MEDIUM";

    @Column(name = "belong_system", length = 30)
    private String belongSystem;

    @Column(name = "source_dept", length = 64)
    private String sourceDept;

    @Column(name = "proposer", length = 64)
    private String proposer;

    @Column(name = "description", columnDefinition = "nvarchar(max)")
    private String description;

    @Column(name = "current_problem", columnDefinition = "nvarchar(max)")
    private String currentProblem;

    @Column(name = "expected_effect", columnDefinition = "nvarchar(max)")
    private String expectedEffect;

    @Column(name = "acceptance_criteria", columnDefinition = "nvarchar(max)")
    private String acceptanceCriteria;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "DRAFT";

    @Column(name = "review_owner", length = 64)
    private String reviewOwner;

    @Column(name = "dev_owner", length = 64)
    private String devOwner;

    @Column(name = "tester", length = 64)
    private String tester;

    @Column(name = "approval_template_id")
    private Long approvalTemplateId;

    @Column(name = "approval_node_idx")
    private Integer approvalNodeIdx = 0;

    @Column(name = "feasibility", length = 1000)
    private String feasibility;

    @Column(name = "workload_estimate", precision = 10, scale = 2)
    private BigDecimal workloadEstimate = BigDecimal.ZERO;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "estimated_hours", precision = 10, scale = 2)
    private BigDecimal estimatedHours = BigDecimal.ZERO;

    @Column(name = "actual_hours", precision = 10, scale = 2)
    private BigDecimal actualHours = BigDecimal.ZERO;

    @Column(name = "progress")
    private Integer progress = 0;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "dev_planned_end")
    private LocalDate devPlannedEnd;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "test_start_date")
    private LocalDate testStartDate;

    @Column(name = "test_end_date")
    private LocalDate testEndDate;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "complete_date")
    private LocalDate completeDate;

    @Column(name = "online_date")
    private LocalDate onlineDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "attachments", columnDefinition = "nvarchar(max)")
    private String attachments;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // --- Getters and Setters ---

    public String getReqNo() { return reqNo; }
    public void setReqNo(String reqNo) { this.reqNo = reqNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getBelongSystem() { return belongSystem; }
    public void setBelongSystem(String belongSystem) { this.belongSystem = belongSystem; }

    public String getSourceDept() { return sourceDept; }
    public void setSourceDept(String sourceDept) { this.sourceDept = sourceDept; }

    public String getProposer() { return proposer; }
    public void setProposer(String proposer) { this.proposer = proposer; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCurrentProblem() { return currentProblem; }
    public void setCurrentProblem(String currentProblem) { this.currentProblem = currentProblem; }

    public String getExpectedEffect() { return expectedEffect; }
    public void setExpectedEffect(String expectedEffect) { this.expectedEffect = expectedEffect; }

    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewOwner() { return reviewOwner; }
    public void setReviewOwner(String reviewOwner) { this.reviewOwner = reviewOwner; }

    public String getDevOwner() { return devOwner; }
    public void setDevOwner(String devOwner) { this.devOwner = devOwner; }

    public String getTester() { return tester; }
    public void setTester(String tester) { this.tester = tester; }

    public Long getApprovalTemplateId() { return approvalTemplateId; }
    public void setApprovalTemplateId(Long approvalTemplateId) { this.approvalTemplateId = approvalTemplateId; }

    public Integer getApprovalNodeIdx() { return approvalNodeIdx; }
    public void setApprovalNodeIdx(Integer approvalNodeIdx) { this.approvalNodeIdx = approvalNodeIdx; }

    public String getFeasibility() { return feasibility; }
    public void setFeasibility(String feasibility) { this.feasibility = feasibility; }

    public BigDecimal getWorkloadEstimate() { return workloadEstimate; }
    public void setWorkloadEstimate(BigDecimal workloadEstimate) { this.workloadEstimate = workloadEstimate; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal estimatedHours) { this.estimatedHours = estimatedHours; }

    public BigDecimal getActualHours() { return actualHours; }
    public void setActualHours(BigDecimal actualHours) { this.actualHours = actualHours; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public void setPlannedStartDate(LocalDate plannedStartDate) { this.plannedStartDate = plannedStartDate; }

    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public void setPlannedEndDate(LocalDate plannedEndDate) { this.plannedEndDate = plannedEndDate; }

    public LocalDate getDevPlannedEnd() { return devPlannedEnd; }
    public void setDevPlannedEnd(LocalDate devPlannedEnd) { this.devPlannedEnd = devPlannedEnd; }

    public LocalDate getActualStartDate() { return actualStartDate; }
    public void setActualStartDate(LocalDate actualStartDate) { this.actualStartDate = actualStartDate; }

    public LocalDate getActualEndDate() { return actualEndDate; }
    public void setActualEndDate(LocalDate actualEndDate) { this.actualEndDate = actualEndDate; }

    public LocalDate getTestStartDate() { return testStartDate; }
    public void setTestStartDate(LocalDate testStartDate) { this.testStartDate = testStartDate; }

    public LocalDate getTestEndDate() { return testEndDate; }
    public void setTestEndDate(LocalDate testEndDate) { this.testEndDate = testEndDate; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public LocalDate getCompleteDate() { return completeDate; }
    public void setCompleteDate(LocalDate completeDate) { this.completeDate = completeDate; }

    public LocalDate getOnlineDate() { return onlineDate; }
    public void setOnlineDate(LocalDate onlineDate) { this.onlineDate = onlineDate; }

    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }

    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}
