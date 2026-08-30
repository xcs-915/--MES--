package com.tns.mes.engineering.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eng_inspection_rule")
public class InspectionRule extends AuditedEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    @Column(name = "name_zh", nullable = false, length = 200)
    private String nameZh;
    @Column(name = "name_en", length = 200)
    private String nameEn;
    @Column(name = "name_ar", length = 200)
    private String nameAr;
    @Column(name = "inspection_type", nullable = false, length = 30)
    private String inspectionType = "IN_PROCESS";
    @Column(name = "sampling_method", nullable = false, length = 30)
    private String samplingMethod = "FULL";
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InspectionItem> items = new ArrayList<>();

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getInspectionType() { return inspectionType; }
    public void setInspectionType(String inspectionType) { this.inspectionType = inspectionType; }
    public String getSamplingMethod() { return samplingMethod; }
    public void setSamplingMethod(String samplingMethod) { this.samplingMethod = samplingMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<InspectionItem> getItems() { return items; }
    public void setItems(List<InspectionItem> items) {
        this.items.clear();
        if (items != null) items.forEach(this::addItem);
    }
    public void addItem(InspectionItem item) { item.setRule(this); this.items.add(item); }
}

