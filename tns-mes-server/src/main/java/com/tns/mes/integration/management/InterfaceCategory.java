package com.tns.mes.integration.management;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Interface Category (接口大类)
 * Groups interfaces by business domain: master data, production, warehouse, quality, equipment.
 */
@Entity
@Table(name = "sys_interface_category")
public class InterfaceCategory extends AuditedEntity {

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(name = "name_zh", length = 128)
    private String nameZh;

    @Column(name = "name_en", length = 128)
    private String nameEn;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
