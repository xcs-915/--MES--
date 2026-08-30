package com.tns.mes.identity.domain;

import com.tns.mes.common.domain.AuditedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/** Configurable page/button action metadata. */
@Entity
@Table(name = "sys_menu_action")
public class MenuAction extends AuditedEntity {
    @Column(name = "menu_code", nullable = false, length = 100) private String menuCode;
    @Column(name = "action_code", nullable = false, length = 100) private String actionCode;
    @Column(name = "name_zh", nullable = false, length = 200) private String nameZh;
    @Column(name = "name_en", length = 200) private String nameEn;
    @Column(name = "name_ar", length = 200) private String nameAr;
    @Column(name = "action_type", nullable = false, length = 30) private String actionType = "BUTTON";
    @Column(name = "permission_code", nullable = false, length = 100) private String permissionCode;
    @Column(name = "sort_order", nullable = false) private Integer sortOrder = 0;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";

    public String getMenuCode(){return menuCode;} public void setMenuCode(String v){menuCode=v;}
    public String getActionCode(){return actionCode;} public void setActionCode(String v){actionCode=v;}
    public String getNameZh(){return nameZh;} public void setNameZh(String v){nameZh=v;}
    public String getNameEn(){return nameEn;} public void setNameEn(String v){nameEn=v;}
    public String getNameAr(){return nameAr;} public void setNameAr(String v){nameAr=v;}
    public String getActionType(){return actionType;} public void setActionType(String v){actionType=v;}
    public String getPermissionCode(){return permissionCode;} public void setPermissionCode(String v){permissionCode=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
