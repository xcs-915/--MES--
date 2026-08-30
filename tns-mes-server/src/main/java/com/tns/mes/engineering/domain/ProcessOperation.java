package com.tns.mes.engineering.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eng_process_operation")
public class ProcessOperation extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private ProcessRoute route;
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;
    @Column(nullable = false, length = 64)
    private String code;
    @Column(name = "name_zh", nullable = false, length = 200)
    private String nameZh;
    @Column(name = "name_en", length = 200)
    private String nameEn;
    @Column(name = "name_ar", length = 200)
    private String nameAr;
    @Column(name = "work_center_id")
    private Long workCenterId;
    @Column(name = "standard_time_seconds", nullable = false)
    private Integer standardTimeSeconds = 0;
    @Column(name = "queue_time_seconds", nullable = false)
    private Integer queueTimeSeconds = 0;
    @Column(name = "is_inspection", nullable = false)
    private Boolean inspection = false;
    @Column(length = 20)
    private String plant;
    @Column(name = "work_center_code", length = 40)
    private String workCenterCode;
    @Column(name = "work_center_internal_id", length = 40)
    private String workCenterInternalId;
    @Column(name = "control_profile", length = 40)
    private String controlProfile;
    @Column(name = "operation_unit", length = 20)
    private String operationUnit;
    @Column(name = "planned_total_quantity", precision = 18, scale = 6)
    private BigDecimal plannedTotalQuantity;
    @Column(name = "planned_yield_quantity", precision = 18, scale = 6)
    private BigDecimal plannedYieldQuantity;
    @Column(name = "planned_scrap_quantity", precision = 18, scale = 6)
    private BigDecimal plannedScrapQuantity;
    @Column(name = "confirmed_yield_quantity", precision = 18, scale = 6)
    private BigDecimal confirmedYieldQuantity;
    @Column(name = "confirmed_scrap_quantity", precision = 18, scale = 6)
    private BigDecimal confirmedScrapQuantity;
    @Column(name = "earliest_start")
    private LocalDateTime earliestStart;
    @Column(name = "earliest_end")
    private LocalDateTime earliestEnd;
    @Column(name = "latest_start")
    private LocalDateTime latestStart;
    @Column(name = "latest_end")
    private LocalDateTime latestEnd;
    @Column(name = "sap_payload", columnDefinition = "nvarchar(max)")
    private String sapPayload;

    public ProcessRoute getRoute() { return route; }
    public void setRoute(ProcessRoute route) { this.route = route; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public Long getWorkCenterId() { return workCenterId; }
    public void setWorkCenterId(Long workCenterId) { this.workCenterId = workCenterId; }
    public Integer getStandardTimeSeconds() { return standardTimeSeconds; }
    public void setStandardTimeSeconds(Integer standardTimeSeconds) { this.standardTimeSeconds = standardTimeSeconds; }
    public Integer getQueueTimeSeconds() { return queueTimeSeconds; }
    public void setQueueTimeSeconds(Integer queueTimeSeconds) { this.queueTimeSeconds = queueTimeSeconds; }
    public Boolean getInspection() { return inspection; }
    public void setInspection(Boolean inspection) { this.inspection = inspection; }
    public String getPlant() { return plant; }
    public void setPlant(String value) { plant = value; }
    public String getWorkCenterCode() { return workCenterCode; }
    public void setWorkCenterCode(String value) { workCenterCode = value; }
    public String getWorkCenterInternalId() { return workCenterInternalId; }
    public void setWorkCenterInternalId(String value) { workCenterInternalId = value; }
    public String getControlProfile() { return controlProfile; }
    public void setControlProfile(String value) { controlProfile = value; }
    public String getOperationUnit() { return operationUnit; }
    public void setOperationUnit(String value) { operationUnit = value; }
    public BigDecimal getPlannedTotalQuantity() { return plannedTotalQuantity; }
    public void setPlannedTotalQuantity(BigDecimal value) { plannedTotalQuantity = value; }
    public BigDecimal getPlannedYieldQuantity() { return plannedYieldQuantity; }
    public void setPlannedYieldQuantity(BigDecimal value) { plannedYieldQuantity = value; }
    public BigDecimal getPlannedScrapQuantity() { return plannedScrapQuantity; }
    public void setPlannedScrapQuantity(BigDecimal value) { plannedScrapQuantity = value; }
    public BigDecimal getConfirmedYieldQuantity() { return confirmedYieldQuantity; }
    public void setConfirmedYieldQuantity(BigDecimal value) { confirmedYieldQuantity = value; }
    public BigDecimal getConfirmedScrapQuantity() { return confirmedScrapQuantity; }
    public void setConfirmedScrapQuantity(BigDecimal value) { confirmedScrapQuantity = value; }
    public LocalDateTime getEarliestStart() { return earliestStart; }
    public void setEarliestStart(LocalDateTime value) { earliestStart = value; }
    public LocalDateTime getEarliestEnd() { return earliestEnd; }
    public void setEarliestEnd(LocalDateTime value) { earliestEnd = value; }
    public LocalDateTime getLatestStart() { return latestStart; }
    public void setLatestStart(LocalDateTime value) { latestStart = value; }
    public LocalDateTime getLatestEnd() { return latestEnd; }
    public void setLatestEnd(LocalDateTime value) { latestEnd = value; }
    public String getSapPayload() { return sapPayload; }
    public void setSapPayload(String value) { sapPayload = value; }
}
