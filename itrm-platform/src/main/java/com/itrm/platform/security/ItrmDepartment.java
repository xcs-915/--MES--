package com.itrm.platform.security;

import com.itrm.platform.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "itrm_department")
public class ItrmDepartment extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "full_path", nullable = false, unique = true, length = 200)
    private String fullPath;

    @Column(name = "parent_path", length = 200)
    private String parentPath;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFullPath() { return fullPath; }
    public void setFullPath(String fullPath) { this.fullPath = fullPath; }
    public String getParentPath() { return parentPath; }
    public void setParentPath(String parentPath) { this.parentPath = parentPath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
