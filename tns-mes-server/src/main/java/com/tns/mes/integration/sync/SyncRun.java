package com.tns.mes.integration.sync;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "int_sync_run")
public class SyncRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private SyncJob job;
    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false, length = 500)
    private String endpoint;
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    @Column(name = "received_count", nullable = false)
    private Integer receivedCount = 0;
    @Column(name = "created_count", nullable = false)
    private Integer createdCount = 0;
    @Column(name = "updated_count", nullable = false)
    private Integer updatedCount = 0;
    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;
    @Column(name = "error_summary", length = 2000)
    private String errorSummary;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public SyncJob getJob() { return job; }
    public void setJob(SyncJob value) { job = value; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String value) { triggerType = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String value) { endpoint = value; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String value) { httpMethod = value; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime value) { startedAt = value; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime value) { finishedAt = value; }
    public Integer getReceivedCount() { return receivedCount; }
    public void setReceivedCount(Integer value) { receivedCount = value; }
    public Integer getCreatedCount() { return createdCount; }
    public void setCreatedCount(Integer value) { createdCount = value; }
    public Integer getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(Integer value) { updatedCount = value; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer value) { failedCount = value; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String value) { errorSummary = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
