package com.tns.mes.engineering.service;

import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.engineering.domain.InspectionItem;
import com.tns.mes.engineering.domain.InspectionRule;
import com.tns.mes.engineering.repo.InspectionRuleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InspectionRuleService {
    private final InspectionRuleRepository repository;
    public InspectionRuleService(InspectionRuleRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public PageResponse<InspectionRule> page(int page, int size) {
        return PageResponse.from(repository.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by("code"))));
    }
    @Transactional(readOnly = true)
    public InspectionRule get(Long id) { return repository.findWithItemsById(id).orElseThrow(() -> new BizException(4041, "error.not-found")); }
    @Transactional
    @Auditable(action = "CREATE", resource = "INSPECTION_RULE")
    public InspectionRule create(InspectionRuleRequest request) {
        validate(request); if (repository.existsByCode(request.getCode())) throw new BizException(4091, "error.duplicate");
        InspectionRule rule = new InspectionRule(); apply(rule, request); return repository.save(rule);
    }
    @Transactional
    @Auditable(action = "UPDATE", resource = "INSPECTION_RULE")
    public InspectionRule update(Long id, InspectionRuleRequest request) {
        InspectionRule rule = get(id); if (!"DRAFT".equals(rule.getStatus())) throw new BizException(4092, "error.invalid-state");
        validate(request); if (!rule.getCode().equals(request.getCode()) && repository.existsByCode(request.getCode())) throw new BizException(4091, "error.duplicate");
        apply(rule, request); return repository.save(rule);
    }
    @Transactional
    @Auditable(action = "PUBLISH", resource = "INSPECTION_RULE")
    public InspectionRule publish(Long id) {
        InspectionRule rule = get(id); if (!"DRAFT".equals(rule.getStatus()) || rule.getItems().isEmpty()) throw new BizException(4092, "error.invalid-state");
        rule.setStatus("ACTIVE"); return repository.save(rule);
    }
    private void validate(InspectionRuleRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException(4003, "error.validation");
        Set<Integer> sequences = new HashSet<>();
        for (InspectionItemRequest item : request.getItems()) if (item.getSequenceNo() == null || !sequences.add(item.getSequenceNo()) || item.getCode() == null || item.getCode().trim().isEmpty() || item.getNameZh() == null || item.getNameZh().trim().isEmpty()) throw new BizException(4003, "error.validation");
    }
    private void apply(InspectionRule rule, InspectionRuleRequest request) {
        rule.setCode(request.getCode().trim()); rule.setNameZh(request.getNameZh().trim()); rule.setNameEn(request.getNameEn()); rule.setNameAr(request.getNameAr()); rule.setInspectionType(request.getInspectionType() == null ? "IN_PROCESS" : request.getInspectionType()); rule.setSamplingMethod(request.getSamplingMethod() == null ? "FULL" : request.getSamplingMethod()); rule.setStatus(request.getStatus() == null ? "DRAFT" : request.getStatus());
        List<InspectionItem> items = new java.util.ArrayList<>();
        for (InspectionItemRequest value : request.getItems()) { InspectionItem item = new InspectionItem(); item.setSequenceNo(value.getSequenceNo()); item.setCode(value.getCode().trim()); item.setNameZh(value.getNameZh().trim()); item.setNameEn(value.getNameEn()); item.setNameAr(value.getNameAr()); item.setSpecification(value.getSpecification()); item.setUnit(value.getUnit()); item.setMinValue(value.getMinValue()); item.setMaxValue(value.getMaxValue()); item.setDataType(value.getDataType() == null ? "TEXT" : value.getDataType()); item.setMandatory(value.getMandatory() == null || value.getMandatory()); items.add(item); }
        rule.setItems(items);
    }
    public static class InspectionRuleRequest {
        @javax.validation.constraints.NotBlank private String code; @javax.validation.constraints.NotBlank private String nameZh; private String nameEn; private String nameAr; private String inspectionType; private String samplingMethod; private String status;
        @javax.validation.constraints.NotEmpty private List<InspectionItemRequest> items;
        public String getCode() { return code; } public void setCode(String value) { code = value; }
        public String getNameZh() { return nameZh; } public void setNameZh(String value) { nameZh = value; }
        public String getNameEn() { return nameEn; } public void setNameEn(String value) { nameEn = value; }
        public String getNameAr() { return nameAr; } public void setNameAr(String value) { nameAr = value; }
        public String getInspectionType() { return inspectionType; } public void setInspectionType(String value) { inspectionType = value; }
        public String getSamplingMethod() { return samplingMethod; } public void setSamplingMethod(String value) { samplingMethod = value; }
        public String getStatus() { return status; } public void setStatus(String value) { status = value; }
        public List<InspectionItemRequest> getItems() { return items; } public void setItems(List<InspectionItemRequest> value) { items = value; }
    }
    public static class InspectionItemRequest {
        @javax.validation.constraints.NotNull private Integer sequenceNo; @javax.validation.constraints.NotBlank private String code; @javax.validation.constraints.NotBlank private String nameZh; private String nameEn; private String nameAr; private String specification; private String unit; private java.math.BigDecimal minValue; private java.math.BigDecimal maxValue; private String dataType; private Boolean mandatory;
        public Integer getSequenceNo() { return sequenceNo; } public void setSequenceNo(Integer value) { sequenceNo = value; }
        public String getCode() { return code; } public void setCode(String value) { code = value; }
        public String getNameZh() { return nameZh; } public void setNameZh(String value) { nameZh = value; }
        public String getNameEn() { return nameEn; } public void setNameEn(String value) { nameEn = value; }
        public String getNameAr() { return nameAr; } public void setNameAr(String value) { nameAr = value; }
        public String getSpecification() { return specification; } public void setSpecification(String value) { specification = value; }
        public String getUnit() { return unit; } public void setUnit(String value) { unit = value; }
        public java.math.BigDecimal getMinValue() { return minValue; } public void setMinValue(java.math.BigDecimal value) { minValue = value; }
        public java.math.BigDecimal getMaxValue() { return maxValue; } public void setMaxValue(java.math.BigDecimal value) { maxValue = value; }
        public String getDataType() { return dataType; } public void setDataType(String value) { dataType = value; }
        public Boolean getMandatory() { return mandatory; } public void setMandatory(Boolean value) { mandatory = value; }
    }
}
