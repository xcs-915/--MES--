package com.tns.mes.integration.outbox;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "mes_outbox_message")
public class OutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;
    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;
    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;
    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String payload;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "last_error", length = 2000)
    private String lastError;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String value) { eventId = value; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String value) { aggregateType = value; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String value) { aggregateId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { eventType = value; }
    public String getPayload() { return payload; }
    public void setPayload(String value) { payload = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer value) { attemptCount = value; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant value) { nextAttemptAt = value; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant value) { publishedAt = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}

