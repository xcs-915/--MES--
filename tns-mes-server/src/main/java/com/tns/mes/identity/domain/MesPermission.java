package com.tns.mes.identity.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "iam_permission")
public class MesPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    @Column(name = "name_zh", nullable = false, length = 200)
    private String nameZh;
    @Column(name = "name_en", length = 200)
    private String nameEn;
    @Column(name = "name_ar", length = 200)
    private String nameAr;
    @Column(name = "permission_type", nullable = false, length = 20)
    private String permissionType = "ACTION";
    @Column(name = "group_code", nullable = false, length = 40)
    private String groupCode = "SYSTEM";
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    @Column(length = 500)
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getPermissionType() { return permissionType; }
    public void setPermissionType(String value) { permissionType = value; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String value) { groupCode = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
}
