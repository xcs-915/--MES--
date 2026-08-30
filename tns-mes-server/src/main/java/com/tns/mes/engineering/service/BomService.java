package com.tns.mes.engineering.service;

import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.engineering.domain.Bom;
import com.tns.mes.engineering.domain.BomItem;
import com.tns.mes.engineering.repo.BomRepository;
import com.tns.mes.engineering.repo.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BomService {
    private final BomRepository repository;
    private final ProductRepository products;

    public BomService(BomRepository repository, ProductRepository products) { this.repository = repository; this.products = products; }

    @Transactional(readOnly = true)
    public PageResponse<Bom> page(Long productId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by("code", "versionCode"));
        return PageResponse.from(productId == null ? repository.findAll(pageable) : repository.findByProductId(productId, pageable));
    }

    @Transactional(readOnly = true)
    public Bom get(Long id) { return repository.findWithItemsById(id).orElseThrow(() -> new BizException(4041, "error.not-found")); }

    @Transactional
    @Auditable(action = "CREATE", resource = "BOM")
    public Bom create(BomRequest request) {
        validateRequest(request);
        if (repository.existsByProductIdAndCodeAndVersionCode(request.getProductId(), request.getCode(), request.getVersionCode()))
            throw new BizException(4091, "error.duplicate");
        Bom bom = new Bom();
        apply(bom, request);
        return repository.save(bom);
    }

    @Transactional
    @Auditable(action = "UPDATE", resource = "BOM")
    public Bom update(Long id, BomRequest request) {
        Bom bom = get(id);
        if (!"DRAFT".equals(bom.getStatus())) throw new BizException(4092, "error.invalid-state");
        validateRequest(request);
        if ((!bom.getProductId().equals(request.getProductId()) || !bom.getCode().equals(request.getCode()) || !bom.getVersionCode().equals(request.getVersionCode()))
                && repository.existsByProductIdAndCodeAndVersionCode(request.getProductId(), request.getCode(), request.getVersionCode()))
            throw new BizException(4091, "error.duplicate");
        apply(bom, request);
        return repository.save(bom);
    }

    @Transactional
    @Auditable(action = "PUBLISH", resource = "BOM")
    public Bom publish(Long id) {
        Bom bom = get(id);
        if (!"DRAFT".equals(bom.getStatus()) || bom.getItems().isEmpty()) throw new BizException(4092, "error.invalid-state");
        bom.setStatus("ACTIVE");
        return repository.save(bom);
    }

    private void validateRequest(BomRequest request) {
        if (!products.existsById(request.getProductId())) throw new BizException(4042, "error.not-found");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BizException(4003, "error.validation");
        Set<Integer> sequences = new HashSet<>();
        for (BomItemRequest item : request.getItems()) {
            if (item.getComponentProductId() == null || !products.existsById(item.getComponentProductId())
                    || item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0
                    || item.getSequenceNo() == null || !sequences.add(item.getSequenceNo())) {
                throw new BizException(4003, "error.validation");
            }
            if (item.getScrapRate() != null && (item.getScrapRate().compareTo(BigDecimal.ZERO) < 0 || item.getScrapRate().compareTo(new BigDecimal("100")) > 0))
                throw new BizException(4003, "error.validation");
        }
    }

    private void apply(Bom bom, BomRequest request) {
        bom.setProductId(request.getProductId()); bom.setCode(request.getCode().trim()); bom.setVersionCode(request.getVersionCode().trim());
        bom.setNameZh(request.getNameZh().trim()); bom.setNameEn(request.getNameEn()); bom.setNameAr(request.getNameAr());
        bom.setStatus(request.getStatus() == null ? "DRAFT" : request.getStatus()); bom.setEffectiveFrom(request.getEffectiveFrom()); bom.setEffectiveTo(request.getEffectiveTo());
        List<BomItem> items = new java.util.ArrayList<>();
        for (BomItemRequest itemRequest : request.getItems()) {
            BomItem item = new BomItem(); item.setComponentProductId(itemRequest.getComponentProductId()); item.setSequenceNo(itemRequest.getSequenceNo());
            item.setQuantity(itemRequest.getQuantity()); item.setScrapRate(itemRequest.getScrapRate() == null ? BigDecimal.ZERO : itemRequest.getScrapRate());
            item.setUnit(itemRequest.getUnit() == null ? "PCS" : itemRequest.getUnit()); item.setIssueMethod(itemRequest.getIssueMethod() == null ? "BACKFLUSH" : itemRequest.getIssueMethod()); items.add(item);
        }
        bom.setItems(items);
    }

    public static class BomRequest {
        @javax.validation.constraints.NotNull private Long productId;
        @javax.validation.constraints.NotBlank private String code;
        @javax.validation.constraints.NotBlank private String versionCode;
        @javax.validation.constraints.NotBlank private String nameZh;
        private String nameEn; private String nameAr; private String status;
        private java.time.LocalDate effectiveFrom; private java.time.LocalDate effectiveTo;
        @javax.validation.constraints.NotEmpty private List<BomItemRequest> items;
        public Long getProductId() { return productId; } public void setProductId(Long value) { productId = value; }
        public String getCode() { return code; } public void setCode(String value) { code = value; }
        public String getVersionCode() { return versionCode; } public void setVersionCode(String value) { versionCode = value; }
        public String getNameZh() { return nameZh; } public void setNameZh(String value) { nameZh = value; }
        public String getNameEn() { return nameEn; } public void setNameEn(String value) { nameEn = value; }
        public String getNameAr() { return nameAr; } public void setNameAr(String value) { nameAr = value; }
        public String getStatus() { return status; } public void setStatus(String value) { status = value; }
        public java.time.LocalDate getEffectiveFrom() { return effectiveFrom; } public void setEffectiveFrom(java.time.LocalDate value) { effectiveFrom = value; }
        public java.time.LocalDate getEffectiveTo() { return effectiveTo; } public void setEffectiveTo(java.time.LocalDate value) { effectiveTo = value; }
        public List<BomItemRequest> getItems() { return items; } public void setItems(List<BomItemRequest> value) { items = value; }
    }
    public static class BomItemRequest {
        @javax.validation.constraints.NotNull private Long componentProductId;
        @javax.validation.constraints.NotNull private Integer sequenceNo;
        @javax.validation.constraints.NotNull private BigDecimal quantity;
        private BigDecimal scrapRate; private String unit; private String issueMethod;
        public Long getComponentProductId() { return componentProductId; } public void setComponentProductId(Long value) { componentProductId = value; }
        public Integer getSequenceNo() { return sequenceNo; } public void setSequenceNo(Integer value) { sequenceNo = value; }
        public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal value) { quantity = value; }
        public BigDecimal getScrapRate() { return scrapRate; } public void setScrapRate(BigDecimal value) { scrapRate = value; }
        public String getUnit() { return unit; } public void setUnit(String value) { unit = value; }
        public String getIssueMethod() { return issueMethod; } public void setIssueMethod(String value) { issueMethod = value; }
    }
}
