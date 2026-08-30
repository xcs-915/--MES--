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
@Table(name = "eng_process_route")
public class ProcessRoute extends AuditedEntity {
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(nullable = false, length = 64)
    private String code;
    @Column(name = "version_code", nullable = false, length = 32)
    private String versionCode;
    @Column(name = "name_zh", nullable = false, length = 200)
    private String nameZh;
    @Column(name = "name_en", length = 200)
    private String nameEn;
    @Column(name = "name_ar", length = 200)
    private String nameAr;
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProcessOperation> operations = new ArrayList<>();

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getVersionCode() { return versionCode; }
    public void setVersionCode(String versionCode) { this.versionCode = versionCode; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<ProcessOperation> getOperations() { return operations; }
    public void setOperations(List<ProcessOperation> operations) {
        this.operations.clear();
        if (operations != null) operations.forEach(this::addOperation);
    }
    public void addOperation(ProcessOperation operation) { operation.setRoute(this); this.operations.add(operation); }
}

