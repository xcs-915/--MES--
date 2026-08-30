package com.tns.mes.identity.domain;

import com.tns.mes.common.domain.AuditedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sys_menu")
public class MenuItem extends AuditedEntity {
    @Column(nullable = false, unique = true, length = 100) private String code;
    @Column(name = "name_zh", nullable = false, length = 200) private String nameZh;
    @Column(name = "name_en", length = 200) private String nameEn;
    @Column(name = "name_ar", length = 200) private String nameAr;
    @Column(name = "parent_code", length = 100) private String parentCode;
    @Column(length = 300) private String path;
    @Column(length = 80) private String icon;
    @Column(name = "permission_code", length = 100) private String permissionCode;
    @Column(nullable = false) private Integer sortOrder = 0;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getNameZh(){return nameZh;} public void setNameZh(String v){nameZh=v;}
    public String getNameEn(){return nameEn;} public void setNameEn(String v){nameEn=v;}
    public String getNameAr(){return nameAr;} public void setNameAr(String v){nameAr=v;}
    public String getParentCode(){return parentCode;} public void setParentCode(String v){parentCode=v;}
    public String getPath(){return path;} public void setPath(String v){path=v;}
    public String getIcon(){return icon;} public void setIcon(String v){icon=v;}
    public String getPermissionCode(){return permissionCode;} public void setPermissionCode(String v){permissionCode=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
