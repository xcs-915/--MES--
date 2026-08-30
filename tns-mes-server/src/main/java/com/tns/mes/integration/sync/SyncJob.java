package com.tns.mes.integration.sync;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "int_sync_job")
public class SyncJob extends AuditedEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    @Column(name = "name_zh", nullable = false, length = 200)
    private String nameZh;
    @Column(name = "name_en", length = 200)
    private String nameEn;
    @Column(name = "name_ar", length = 200)
    private String nameAr;
    @Column(name = "system_code", nullable = false, length = 40)
    private String systemCode;
    @Column(nullable = false, length = 500)
    private String endpoint;
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod = "GET";
    @Column(name = "cron_expression", nullable = false, length = 80)
    private String cronExpression;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false)
    private Boolean enabled = false;
    @Column(nullable = false, length = 20)
    private String status = "IDLE";
    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;
    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String value) { nameZh = value; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String value) { nameEn = value; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String value) { nameAr = value; }
    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String value) { systemCode = value; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String value) { endpoint = value; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String value) { httpMethod = value; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String value) { cronExpression = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean value) { enabled = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime value) { lastRunAt = value; }
    public LocalDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(LocalDateTime value) { nextRunAt = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
}
