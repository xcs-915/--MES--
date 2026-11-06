package com.itrm.platform.domain;

import com.itrm.platform.common.BaseEntity;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "itrm_approval_template")
public class ApprovalTemplate extends BaseEntity {

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "belong_system", length = 30)
    private String belongSystem;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @Transient
    private List<ApprovalNode> nodes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBelongSystem() { return belongSystem; }
    public void setBelongSystem(String belongSystem) { this.belongSystem = belongSystem; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<ApprovalNode> getNodes() { return nodes; }
    public void setNodes(List<ApprovalNode> nodes) { this.nodes = nodes; }
}
