package com.tns.mes.identity.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "iam_role")
public class MesRole extends AuditedEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    @Column(name = "name_zh", nullable = false, length = 200)
    private String nameZh;
    @Column(name = "name_en", length = 200)
    private String nameEn;
    @Column(name = "name_ar", length = 200)
    private String nameAr;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "iam_role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<MesPermission> permissions = new HashSet<>();

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Set<MesPermission> getPermissions() { return permissions; }
    public void setPermissions(Set<MesPermission> permissions) { this.permissions = permissions; }
}

