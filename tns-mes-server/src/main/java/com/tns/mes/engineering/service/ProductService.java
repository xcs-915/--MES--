package com.tns.mes.engineering.service;

import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.engineering.domain.Product;
import com.tns.mes.engineering.repo.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public PageResponse<Product> page(String keyword, String productType, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.ASC, "code"));
        Specification<Product> spec = Specification.where(null);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String term = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("code")), term),
                    cb.like(cb.lower(root.get("nameZh")), term),
                    cb.like(cb.lower(root.get("nameEn")), term),
                    cb.like(cb.lower(root.get("specification")), term)));
        }
        if (productType != null && !productType.trim().isEmpty())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("productType"), productType.trim()));
        if (status != null && !status.trim().isEmpty())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase()));
        Page<Product> result = repository.findAll(spec, pageable);
        return PageResponse.from(result);
    }

    public PageResponse<Product> page(String keyword, int page, int size) {
        return page(keyword, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public Product get(Long id) { return repository.findById(id).orElseThrow(() -> new BizException(4041, "error.not-found")); }

    @Transactional
    @Auditable(action = "CREATE", resource = "PRODUCT")
    public Product create(ProductRequest request) {
        String code = request.getCode().trim();
        if (repository.existsByCode(code)) throw new BizException(4091, "error.duplicate");
        Product product = new Product();
        apply(product, request);
        return repository.save(product);
    }

    @Transactional
    @Auditable(action = "UPDATE", resource = "PRODUCT")
    public Product update(Long id, ProductRequest request) {
        Product product = get(id);
        String code = request.getCode().trim();
        if (!code.equals(product.getCode()) && repository.existsByCode(code)) throw new BizException(4091, "error.duplicate");
        apply(product, request);
        return repository.save(product);
    }

    @Transactional
    @Auditable(action = "DELETE", resource = "PRODUCT")
    public void delete(Long id) {
        Product product = get(id);
        product.setStatus("INACTIVE");
        repository.save(product);
    }

    private void apply(Product product, ProductRequest request) {
        product.setCode(request.getCode().trim());
        product.setNameZh(request.getNameZh().trim());
        product.setNameEn(request.getNameEn());
        product.setNameAr(request.getNameAr());
        product.setProductType(request.getProductType() == null ? "FINISHED" : request.getProductType());
        product.setUnit(request.getUnit() == null ? "PCS" : request.getUnit());
        product.setSpecification(request.getSpecification());
        product.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        product.setTraceable(request.getTraceable() == null || request.getTraceable());
        product.setSource(request.getSource());
        product.setProductOldId(request.getProductOldId());
        product.setProductGroup(request.getProductGroup());
        product.setGrossWeight(request.getGrossWeight());
        product.setNetWeight(request.getNetWeight());
        product.setWeightUnit(request.getWeightUnit());
        product.setCountryOfOrigin(request.getCountryOfOrigin());
        product.setProductHierarchy(request.getProductHierarchy());
        product.setDivisionCode(request.getDivisionCode());
        product.setManufacturerNumber(request.getManufacturerNumber());
        product.setManufacturerPartNumber(request.getManufacturerPartNumber());
        product.setMaterialRevisionLevel(request.getMaterialRevisionLevel());
        product.setSerialNumberProfile(request.getSerialNumberProfile());
        product.setBatchManaged(request.getBatchManaged() != null ? request.getBatchManaged() : false);
        product.setMarkedForDeletion(request.getMarkedForDeletion() != null ? request.getMarkedForDeletion() : false);
        product.setBrand(request.getBrand());
        product.setColor(request.getColor());
        product.setCustomerPartNumber(request.getCustomerPartNumber());
        product.setProductModel(request.getProductModel());
        product.setDrawingNumber(request.getDrawingNumber());
        product.setMinPackagingQty(request.getMinPackagingQty());
    }

    public static class ProductRequest {
        @javax.validation.constraints.NotBlank private String code;
        @javax.validation.constraints.NotBlank private String nameZh;
        private String nameEn;
        private String nameAr;
        private String productType;
        private String unit;
        private String specification;
        private String status;
        private Boolean traceable;
        private String source;
        private String productOldId;
        private String productGroup;
        private java.math.BigDecimal grossWeight;
        private java.math.BigDecimal netWeight;
        private String weightUnit;
        private String countryOfOrigin;
        private String productHierarchy;
        private String divisionCode;
        private String manufacturerNumber;
        private String manufacturerPartNumber;
        private String materialRevisionLevel;
        private String serialNumberProfile;
        private Boolean batchManaged;
        private Boolean markedForDeletion;
        private String brand;
        private String color;
        private String customerPartNumber;
        private String productModel;
        private String drawingNumber;
        private java.math.BigDecimal minPackagingQty;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getNameZh() { return nameZh; }
        public void setNameZh(String nameZh) { this.nameZh = nameZh; }
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        public String getNameAr() { return nameAr; }
        public void setNameAr(String nameAr) { this.nameAr = nameAr; }
        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getSpecification() { return specification; }
        public void setSpecification(String specification) { this.specification = specification; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Boolean getTraceable() { return traceable; }
        public void setTraceable(Boolean traceable) { this.traceable = traceable; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getProductOldId() { return productOldId; }
        public void setProductOldId(String productOldId) { this.productOldId = productOldId; }
        public String getProductGroup() { return productGroup; }
        public void setProductGroup(String productGroup) { this.productGroup = productGroup; }
        public java.math.BigDecimal getGrossWeight() { return grossWeight; }
        public void setGrossWeight(java.math.BigDecimal grossWeight) { this.grossWeight = grossWeight; }
        public java.math.BigDecimal getNetWeight() { return netWeight; }
        public void setNetWeight(java.math.BigDecimal netWeight) { this.netWeight = netWeight; }
        public String getWeightUnit() { return weightUnit; }
        public void setWeightUnit(String weightUnit) { this.weightUnit = weightUnit; }
        public String getCountryOfOrigin() { return countryOfOrigin; }
        public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }
        public String getProductHierarchy() { return productHierarchy; }
        public void setProductHierarchy(String productHierarchy) { this.productHierarchy = productHierarchy; }
        public String getDivisionCode() { return divisionCode; }
        public void setDivisionCode(String divisionCode) { this.divisionCode = divisionCode; }
        public String getManufacturerNumber() { return manufacturerNumber; }
        public void setManufacturerNumber(String manufacturerNumber) { this.manufacturerNumber = manufacturerNumber; }
        public String getManufacturerPartNumber() { return manufacturerPartNumber; }
        public void setManufacturerPartNumber(String manufacturerPartNumber) { this.manufacturerPartNumber = manufacturerPartNumber; }
        public String getMaterialRevisionLevel() { return materialRevisionLevel; }
        public void setMaterialRevisionLevel(String materialRevisionLevel) { this.materialRevisionLevel = materialRevisionLevel; }
        public String getSerialNumberProfile() { return serialNumberProfile; }
        public void setSerialNumberProfile(String serialNumberProfile) { this.serialNumberProfile = serialNumberProfile; }
        public Boolean getBatchManaged() { return batchManaged; }
        public void setBatchManaged(Boolean batchManaged) { this.batchManaged = batchManaged; }
        public Boolean getMarkedForDeletion() { return markedForDeletion; }
        public void setMarkedForDeletion(Boolean markedForDeletion) { this.markedForDeletion = markedForDeletion; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getCustomerPartNumber() { return customerPartNumber; }
        public void setCustomerPartNumber(String customerPartNumber) { this.customerPartNumber = customerPartNumber; }
        public String getProductModel() { return productModel; }
        public void setProductModel(String productModel) { this.productModel = productModel; }
        public String getDrawingNumber() { return drawingNumber; }
        public void setDrawingNumber(String drawingNumber) { this.drawingNumber = drawingNumber; }
        public java.math.BigDecimal getMinPackagingQty() { return minPackagingQty; }
        public void setMinPackagingQty(java.math.BigDecimal minPackagingQty) { this.minPackagingQty = minPackagingQty; }
    }
}
