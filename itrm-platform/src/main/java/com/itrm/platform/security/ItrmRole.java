package com.itrm.platform.security;

import com.itrm.platform.common.BaseEntity;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "itrm_role")
public class ItrmRole extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "built_in", nullable = false)
    private Boolean builtIn = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "itrm_role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @OrderBy("sortOrder ASC")
    private Set<ItrmPermission> permissions = new LinkedHashSet<>();

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getBuiltIn() { return builtIn; }
    public void setBuiltIn(Boolean builtIn) { this.builtIn = builtIn; }
    public Set<ItrmPermission> getPermissions() { return permissions; }
    public void setPermissions(Set<ItrmPermission> permissions) { this.permissions = permissions; }
}
