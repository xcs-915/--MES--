package com.tns.mes.engineering.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "eng_esop_review_log")
public class EsopReviewLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long value) { documentId = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getActor() { return actor; }
    public void setActor(String value) { actor = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
