package com.tns.mes.integration.management;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Interface Definition (接口定义)
 * A specific API endpoint belonging to a category and targeting an external system.
 * Example: "上料校验" belongs to category=PRODUCTION, system=MIDDLEWARE.
 */
@Entity
@Table(name = "sys_interface_def")
public class InterfaceDefinition extends AuditedEntity {

    @Column(name = "category_code", nullable = false, length = 64)
    private String categoryCode;

    @Column(name = "system_code", nullable = false, length = 64)
    private String systemCode;

    @Column(nullable = false, length = 128, unique = true)
    private String code;

    @Column(name = "name_zh", length = 256)
    private String nameZh;

    @Column(name = "name_en", length = 256)
    private String nameEn;

    @Column(nullable = false, length = 8)
    private String method = "GET";

    @Column(length = 512)
    private String path;

    @Column(name = "request_template", columnDefinition = "NVARCHAR(MAX)")
    private String requestTemplate;

    @Column(name = "response_mapping", columnDefinition = "NVARCHAR(MAX)")
    private String responseMapping;

    @Column(name = "sync_direction", length = 16)
    private String syncDirection = "INBOUND";

    @Column(name = "schedule_cron", length = 64)
    private String scheduleCron;

    @Column(length = 512)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getRequestTemplate() { return requestTemplate; }
    public void setRequestTemplate(String requestTemplate) { this.requestTemplate = requestTemplate; }
    public String getResponseMapping() { return responseMapping; }
    public void setResponseMapping(String responseMapping) { this.responseMapping = responseMapping; }
    public String getSyncDirection() { return syncDirection; }
    public void setSyncDirection(String syncDirection) { this.syncDirection = syncDirection; }
    public String getScheduleCron() { return scheduleCron; }
    public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
