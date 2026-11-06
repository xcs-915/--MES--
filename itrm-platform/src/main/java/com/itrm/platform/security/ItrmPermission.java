package com.itrm.platform.security;

import com.itrm.platform.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "itrm_permission")
public class ItrmPermission extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 40)
    private String module;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
