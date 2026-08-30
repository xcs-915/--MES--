package com.tns.mes.basic.service;

import com.tns.mes.basic.domain.MasterData;
import com.tns.mes.basic.domain.MasterDataType;
import com.tns.mes.basic.repo.MasterDataRepository;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterDataService {
    private final MasterDataRepository repository;

    public MasterDataService(MasterDataRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public PageResponse<MasterData> page(String type, String keyword, int page, int size) {
        String dataType = parse(type).getCode();
        PageRequest request = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by("code")));
        Page<MasterData> result = keyword == null || keyword.trim().isEmpty()
                ? repository.findByDataType(dataType, request)
                : repository.findByDataTypeAndCodeContainingIgnoreCase(dataType, keyword.trim(), request);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public MasterData get(String type, Long id) {
        MasterData value = repository.findById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
        ensureType(type, value);
        return value;
    }

    @Transactional
    @Auditable(action = "CREATE", resource = "MASTER_DATA")
    public MasterData create(String type, MasterDataRequest request) {
        String dataType = parse(type).getCode();
        if (repository.existsByDataTypeAndCode(dataType, request.getCode())) {
            throw new BizException(4091, "error.duplicate");
        }
        validateParent(dataType, request.getParentId(), null);
        MasterData value = new MasterData();
        apply(value, dataType, request);
        return repository.save(value);
    }

    @Transactional
    @Auditable(action = "UPDATE", resource = "MASTER_DATA")
    public MasterData update(String type, Long id, MasterDataRequest request) {
        MasterData value = get(type, id);
        if (!value.getCode().equals(request.getCode()) && repository.existsByDataTypeAndCode(parse(type).getCode(), request.getCode())) {
            throw new BizException(4091, "error.duplicate");
        }
        validateParent(parse(type).getCode(), request.getParentId(), id);
        apply(value, parse(type).getCode(), request);
        return repository.save(value);
    }

    @Transactional
    @Auditable(action = "DELETE", resource = "MASTER_DATA")
    public void delete(String type, Long id) {
        MasterData value = get(type, id);
        value.setStatus("INACTIVE");
        repository.save(value);
    }

    private void apply(MasterData value, String dataType, MasterDataRequest request) {
        value.setDataType(dataType);
        value.setCode(request.getCode().trim());
        value.setNameZh(request.getNameZh().trim());
        value.setNameEn(request.getNameEn());
        value.setNameAr(request.getNameAr());
        value.setParentId(request.getParentId());
        value.setDescription(request.getDescription());
        value.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        value.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        value.setAttributes(request.getAttributes());
    }

    private MasterDataType parse(String type) {
        try { return MasterDataType.parse(type); }
        catch (IllegalArgumentException ex) { throw new BizException(4002, "master.type.required"); }
    }

    private void ensureType(String type, MasterData value) {
        if (!parse(type).getCode().equals(value.getDataType())) throw new BizException(4041, "error.not-found");
    }

    private void validateParent(String dataType, Long parentId, Long currentId) {
        if (parentId == null) return;
        if (parentId.equals(currentId)) throw new BizException(4003, "error.validation");
        MasterData parent = repository.findById(parentId).orElseThrow(() -> new BizException(4042, "error.not-found"));
        String parentType = parent.getDataType();
        boolean allowed;
        switch (dataType) {
            case "factory": allowed = "enterprise".equals(parentType); break;
            case "workshop": allowed = "factory".equals(parentType); break;
            case "department": allowed = "enterprise".equals(parentType) || "factory".equals(parentType) || "workshop".equals(parentType); break;
            case "warehouse": allowed = "factory".equals(parentType); break;
            case "work-center": allowed = "factory".equals(parentType) || "workshop".equals(parentType); break;
            case "production-line": allowed = "factory".equals(parentType) || "workshop".equals(parentType); break;
            case "workstation": allowed = "production-line".equals(parentType) || "work-center".equals(parentType); break;
            case "person": allowed = "department".equals(parentType); break;
            case "position": allowed = "department".equals(parentType); break;
            default: allowed = false;
        }
        if (!allowed) throw new BizException(4003, "error.validation");
    }

    public static class MasterDataRequest {
        @javax.validation.constraints.NotBlank(message = "code is required")
        private String code;
        @javax.validation.constraints.NotBlank(message = "nameZh is required")
        private String nameZh;
        private String nameEn;
        private String nameAr;
        private Long parentId;
        private String description;
        private String status;
        private Integer sortOrder;
        private String attributes;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getNameZh() { return nameZh; }
        public void setNameZh(String nameZh) { this.nameZh = nameZh; }
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        public String getNameAr() { return nameAr; }
        public void setNameAr(String nameAr) { this.nameAr = nameAr; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getAttributes() { return attributes; }
        public void setAttributes(String attributes) { this.attributes = attributes; }
    }
}
