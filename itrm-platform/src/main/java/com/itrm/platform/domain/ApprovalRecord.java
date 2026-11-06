package com.itrm.platform.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "itrm_approval_record")
public class ApprovalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requirement_id", nullable = false)
    private Long requirementId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "node_idx", nullable = false)
    private Integer nodeIdx;

    @Column(name = "node_name", length = 100, nullable = false)
    private String nodeName;

    @Column(name = "node_type", length = 30, nullable = false)
    private String nodeType;

    @Column(name = "approver", length = 64)
    private String approver;

    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "acted_at")
    private Instant actedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Integer getNodeIdx() { return nodeIdx; }
    public void setNodeIdx(Integer nodeIdx) { this.nodeIdx = nodeIdx; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getActedAt() { return actedAt; }
    public void setActedAt(Instant actedAt) { this.actedAt = actedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
