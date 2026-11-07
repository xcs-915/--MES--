package com.itrm.platform.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "itrm_notification")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64) private String recipient;
    @Column(name = "requirement_id") private Long requirementId;
    @Column(name = "event_type", nullable = false, length = 50) private String eventType;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 1000) private String content;
    @Column(name = "read_flag", nullable = false) private Boolean readFlag = false;
    @Column(name = "wecom_status", length = 20) private String wecomStatus;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    public Long getId() { return id; }
    public String getRecipient() { return recipient; } public void setRecipient(String value) { recipient = value; }
    public Long getRequirementId() { return requirementId; } public void setRequirementId(Long value) { requirementId = value; }
    public String getEventType() { return eventType; } public void setEventType(String value) { eventType = value; }
    public String getTitle() { return title; } public void setTitle(String value) { title = value; }
    public String getContent() { return content; } public void setContent(String value) { content = value; }
    public Boolean getReadFlag() { return readFlag; } public void setReadFlag(Boolean value) { readFlag = value; }
    public String getWecomStatus() { return wecomStatus; } public void setWecomStatus(String value) { wecomStatus = value; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant value) { createdAt = value; }
}
