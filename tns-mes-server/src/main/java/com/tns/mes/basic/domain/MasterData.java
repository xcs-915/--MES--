package com.tns.mes.basic.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "md_master_data")
public class MasterData extends AuditedEntity {
    @Column(name = "data_type", nullable = false, length = 40)
    private String dataType;
    @Column(nullable = false, length = 64)
    private String code;
    @Column(name = "name_zh", nullable = false, length = 200)
    private String nameZh;
    @Column(name = "name_en", length = 200)
    private String nameEn;
    @Column(name = "name_ar", length = 200)
    private String nameAr;
    @Column(name = "parent_id")
    private Long parentId;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    @Column(length = 4000)
    private String attributes;

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }
}

