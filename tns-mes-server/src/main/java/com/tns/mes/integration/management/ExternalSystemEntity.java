package com.tns.mes.integration.management;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * External System (外部系统)
 * Represents an external system that MES integrates with: SAP, Middleware, WMS, etc.
 */
@Entity
@Table(name = "sys_external_system")
public class ExternalSystemEntity extends AuditedEntity {

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(name = "name_zh", length = 128)
    private String nameZh;

    @Column(name = "name_en", length = 128)
    private String nameEn;

    @Column(name = "base_url", length = 512)
    private String baseUrl;

    @Column(name = "auth_type", length = 32)
    private String authType = "BASIC";

    @Column(name = "auth_config", columnDefinition = "NVARCHAR(MAX)")
    private String authConfig;

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
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getAuthConfig() { return authConfig; }
    public void setAuthConfig(String authConfig) { this.authConfig = authConfig; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
