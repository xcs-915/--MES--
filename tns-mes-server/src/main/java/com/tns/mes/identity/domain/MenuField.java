package com.tns.mes.identity.domain;

import com.tns.mes.common.domain.AuditedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/** Configurable field metadata for a page menu. */
@Entity
@Table(name = "sys_menu_field")
public class MenuField extends AuditedEntity {
    @Column(name = "menu_code", nullable = false, length = 100) private String menuCode;
    @Column(name = "field_code", nullable = false, length = 100) private String fieldCode;
    @Column(name = "field_path", nullable = false, length = 200) private String fieldPath;
    @Column(name = "label_zh", nullable = false, length = 200) private String labelZh;
    @Column(name = "label_en", length = 200) private String labelEn;
    @Column(name = "label_ar", length = 200) private String labelAr;
    @Column(name = "field_type", nullable = false, length = 30) private String fieldType = "TEXT";
    @Column(name = "visible_list", nullable = false) private Boolean visibleList = true;
    @Column(name = "visible_detail", nullable = false) private Boolean visibleDetail = true;
    @Column(name = "queryable", nullable = false) private Boolean queryable = false;
    @Column(name = "default_visible", nullable = false) private Boolean defaultVisible = true;
    @Column(name = "sort_order", nullable = false) private Integer sortOrder = 0;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";

    public String getMenuCode(){return menuCode;} public void setMenuCode(String v){menuCode=v;}
    public String getFieldCode(){return fieldCode;} public void setFieldCode(String v){fieldCode=v;}
    public String getFieldPath(){return fieldPath;} public void setFieldPath(String v){fieldPath=v;}
    public String getLabelZh(){return labelZh;} public void setLabelZh(String v){labelZh=v;}
    public String getLabelEn(){return labelEn;} public void setLabelEn(String v){labelEn=v;}
    public String getLabelAr(){return labelAr;} public void setLabelAr(String v){labelAr=v;}
    public String getFieldType(){return fieldType;} public void setFieldType(String v){fieldType=v;}
    public Boolean getVisibleList(){return visibleList;} public void setVisibleList(Boolean v){visibleList=v;}
    public Boolean getVisibleDetail(){return visibleDetail;} public void setVisibleDetail(Boolean v){visibleDetail=v;}
    public Boolean getQueryable(){return queryable;} public void setQueryable(Boolean v){queryable=v;}
    public Boolean getDefaultVisible(){return defaultVisible;} public void setDefaultVisible(Boolean v){defaultVisible=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
