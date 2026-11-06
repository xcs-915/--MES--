package com.itrm.platform.domain;

import com.itrm.platform.common.BaseEntity;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "itrm_test_record")
public class TestRecord extends BaseEntity {

    @Column(name = "requirement_id", nullable = false)
    private Long requirementId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "type", length = 20)
    private String type = "TEST";

    @Column(name = "severity", length = 20)
    private String severity = "LOW";

    @Column(name = "status", length = 20)
    private String status = "OPEN";

    @Column(name = "tester", length = 64)
    private String tester;

    @Column(name = "handler", length = 64)
    private String handler;

    @Column(name = "description", columnDefinition = "nvarchar(max)")
    private String description;

    @Column(name = "expected_result", length = 1000)
    private String expectedResult;

    @Column(name = "actual_result", length = 1000)
    private String actualResult;

    @Column(name = "accept_result", length = 20)
    private String acceptResult;

    @Column(name = "accept_by", length = 64)
    private String acceptBy;

    @Column(name = "accept_at")
    private Instant acceptAt;

    @Column(name = "attachments", columnDefinition = "nvarchar(max)")
    private String attachments;

    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTester() { return tester; }
    public void setTester(String tester) { this.tester = tester; }
    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }
    public String getAcceptResult() { return acceptResult; }
    public void setAcceptResult(String acceptResult) { this.acceptResult = acceptResult; }
    public String getAcceptBy() { return acceptBy; }
    public void setAcceptBy(String acceptBy) { this.acceptBy = acceptBy; }
    public Instant getAcceptAt() { return acceptAt; }
    public void setAcceptAt(Instant acceptAt) { this.acceptAt = acceptAt; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
}
