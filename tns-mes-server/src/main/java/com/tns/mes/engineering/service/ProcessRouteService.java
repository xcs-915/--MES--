package com.tns.mes.engineering.service;

import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.engineering.domain.ProcessOperation;
import com.tns.mes.engineering.domain.ProcessRoute;
import com.tns.mes.engineering.repo.ProcessRouteRepository;
import com.tns.mes.engineering.repo.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProcessRouteService {
    private final ProcessRouteRepository repository;
    private final ProductRepository products;

    public ProcessRouteService(ProcessRouteRepository repository, ProductRepository products) { this.repository = repository; this.products = products; }

    @Transactional(readOnly = true)
    public PageResponse<ProcessRoute> page(Long productId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by("code", "versionCode"));
        return PageResponse.from(productId == null ? repository.findAll(pageable) : repository.findByProductId(productId, pageable));
    }

    @Transactional(readOnly = true)
    public ProcessRoute get(Long id) { return repository.findWithOperationsById(id).orElseThrow(() -> new BizException(4041, "error.not-found")); }

    @Transactional
    @Auditable(action = "CREATE", resource = "PROCESS_ROUTE")
    public ProcessRoute create(ProcessRouteRequest request) {
        validate(request);
        if (repository.existsByProductIdAndCodeAndVersionCode(request.getProductId(), request.getCode(), request.getVersionCode())) throw new BizException(4091, "error.duplicate");
        ProcessRoute route = new ProcessRoute(); apply(route, request); return repository.save(route);
    }

    @Transactional
    @Auditable(action = "UPDATE", resource = "PROCESS_ROUTE")
    public ProcessRoute update(Long id, ProcessRouteRequest request) {
        ProcessRoute route = get(id);
        if (!"DRAFT".equals(route.getStatus())) throw new BizException(4092, "error.invalid-state");
        validate(request);
        if ((!route.getProductId().equals(request.getProductId()) || !route.getCode().equals(request.getCode()) || !route.getVersionCode().equals(request.getVersionCode()))
                && repository.existsByProductIdAndCodeAndVersionCode(request.getProductId(), request.getCode(), request.getVersionCode())) throw new BizException(4091, "error.duplicate");
        apply(route, request); return repository.save(route);
    }

    @Transactional
    @Auditable(action = "PUBLISH", resource = "PROCESS_ROUTE")
    public ProcessRoute publish(Long id) {
        ProcessRoute route = get(id);
        if (!"DRAFT".equals(route.getStatus()) || route.getOperations().isEmpty()) throw new BizException(4092, "error.invalid-state");
        route.setStatus("ACTIVE"); return repository.save(route);
    }

    private void validate(ProcessRouteRequest request) {
        if (!products.existsById(request.getProductId()) || request.getOperations() == null || request.getOperations().isEmpty()) throw new BizException(4003, "error.validation");
        Set<Integer> sequences = new HashSet<>(); Set<String> codes = new HashSet<>();
        for (OperationRequest op : request.getOperations()) {
            if (op.getSequenceNo() == null || !sequences.add(op.getSequenceNo()) || op.getCode() == null || !codes.add(op.getCode()) || op.getNameZh() == null || op.getNameZh().trim().isEmpty()) throw new BizException(4003, "error.validation");
            if (op.getStandardTimeSeconds() != null && op.getStandardTimeSeconds() < 0) throw new BizException(4003, "error.validation");
        }
    }

    private void apply(ProcessRoute route, ProcessRouteRequest request) {
        route.setProductId(request.getProductId()); route.setCode(request.getCode().trim()); route.setVersionCode(request.getVersionCode().trim());
        route.setNameZh(request.getNameZh().trim()); route.setNameEn(request.getNameEn()); route.setNameAr(request.getNameAr()); route.setStatus(request.getStatus() == null ? "DRAFT" : request.getStatus());
        List<ProcessOperation> operations = new java.util.ArrayList<>();
        for (OperationRequest value : request.getOperations()) {
            ProcessOperation op = new ProcessOperation(); op.setSequenceNo(value.getSequenceNo()); op.setCode(value.getCode().trim()); op.setNameZh(value.getNameZh().trim()); op.setNameEn(value.getNameEn()); op.setNameAr(value.getNameAr());
            op.setWorkCenterId(value.getWorkCenterId()); op.setStandardTimeSeconds(value.getStandardTimeSeconds() == null ? 0 : value.getStandardTimeSeconds()); op.setQueueTimeSeconds(value.getQueueTimeSeconds() == null ? 0 : value.getQueueTimeSeconds()); op.setInspection(value.getInspection() != null && value.getInspection()); operations.add(op);
        }
        route.setOperations(operations);
    }

    public static class ProcessRouteRequest {
        @javax.validation.constraints.NotNull private Long productId; @javax.validation.constraints.NotBlank private String code;
        @javax.validation.constraints.NotBlank private String versionCode; @javax.validation.constraints.NotBlank private String nameZh;
        private String nameEn; private String nameAr; private String status;
        @javax.validation.constraints.NotEmpty private List<OperationRequest> operations;
        public Long getProductId() { return productId; } public void setProductId(Long value) { productId = value; }
        public String getCode() { return code; } public void setCode(String value) { code = value; }
        public String getVersionCode() { return versionCode; } public void setVersionCode(String value) { versionCode = value; }
        public String getNameZh() { return nameZh; } public void setNameZh(String value) { nameZh = value; }
        public String getNameEn() { return nameEn; } public void setNameEn(String value) { nameEn = value; }
        public String getNameAr() { return nameAr; } public void setNameAr(String value) { nameAr = value; }
        public String getStatus() { return status; } public void setStatus(String value) { status = value; }
        public List<OperationRequest> getOperations() { return operations; } public void setOperations(List<OperationRequest> value) { operations = value; }
    }
    public static class OperationRequest {
        @javax.validation.constraints.NotNull private Integer sequenceNo; @javax.validation.constraints.NotBlank private String code; @javax.validation.constraints.NotBlank private String nameZh;
        private String nameEn; private String nameAr; private Long workCenterId; private Integer standardTimeSeconds; private Integer queueTimeSeconds; private Boolean inspection;
        public Integer getSequenceNo() { return sequenceNo; } public void setSequenceNo(Integer value) { sequenceNo = value; }
        public String getCode() { return code; } public void setCode(String value) { code = value; }
        public String getNameZh() { return nameZh; } public void setNameZh(String value) { nameZh = value; }
        public String getNameEn() { return nameEn; } public void setNameEn(String value) { nameEn = value; }
        public String getNameAr() { return nameAr; } public void setNameAr(String value) { nameAr = value; }
        public Long getWorkCenterId() { return workCenterId; } public void setWorkCenterId(Long value) { workCenterId = value; }
        public Integer getStandardTimeSeconds() { return standardTimeSeconds; } public void setStandardTimeSeconds(Integer value) { standardTimeSeconds = value; }
        public Integer getQueueTimeSeconds() { return queueTimeSeconds; } public void setQueueTimeSeconds(Integer value) { queueTimeSeconds = value; }
        public Boolean getInspection() { return inspection; } public void setInspection(Boolean value) { inspection = value; }
    }
}
