package com.tns.mes.integration.management;

import com.tns.mes.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Interface Management Service
 * CRUD operations for interface categories, external systems, and interface definitions.
 * This is the central registry for all external system integrations.
 */
@Service
public class InterfaceManagementService {

    private final InterfaceCategoryRepository categoryRepo;
    private final ExternalSystemRepository systemRepo;
    private final InterfaceDefinitionRepository defRepo;

    public InterfaceManagementService(InterfaceCategoryRepository categoryRepo,
                                      ExternalSystemRepository systemRepo,
                                      InterfaceDefinitionRepository defRepo) {
        this.categoryRepo = categoryRepo;
        this.systemRepo = systemRepo;
        this.defRepo = defRepo;
    }

    // ===== Category =====

    public List<InterfaceCategory> listCategories() {
        return categoryRepo.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public InterfaceCategory createCategory(InterfaceCategory cat) {
        if (cat.getCode() == null || cat.getCode().trim().isEmpty())
            throw new BizException(4001, "error.validation");
        if (categoryRepo.existsByCode(cat.getCode()))
            throw new BizException(4002, "error.duplicate");
        return categoryRepo.save(cat);
    }

    @Transactional
    public InterfaceCategory updateCategory(Long id, InterfaceCategory cat) {
        InterfaceCategory existing = categoryRepo.findById(id)
                .orElseThrow(() -> new BizException(4040, "error.notFound"));
        if (cat.getNameZh() != null) existing.setNameZh(cat.getNameZh());
        if (cat.getNameEn() != null) existing.setNameEn(cat.getNameEn());
        if (cat.getSortOrder() != null) existing.setSortOrder(cat.getSortOrder());
        if (cat.getStatus() != null) existing.setStatus(cat.getStatus());
        return categoryRepo.save(existing);
    }

    @Transactional
    public void deleteCategory(Long id) {
        InterfaceCategory cat = categoryRepo.findById(id)
                .orElseThrow(() -> new BizException(4040, "error.notFound"));
        long count = defRepo.countByCategoryCode(cat.getCode());
        if (count > 0)
            throw new BizException(4009, "error.inUse");
        categoryRepo.delete(cat);
    }

    // ===== External System =====

    public List<ExternalSystemEntity> listSystems() {
        return systemRepo.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public ExternalSystemEntity createSystem(ExternalSystemEntity sys) {
        if (sys.getCode() == null || sys.getCode().trim().isEmpty())
            throw new BizException(4001, "error.validation");
        if (systemRepo.existsByCode(sys.getCode()))
            throw new BizException(4002, "error.duplicate");
        return systemRepo.save(sys);
    }

    @Transactional
    public ExternalSystemEntity updateSystem(Long id, ExternalSystemEntity sys) {
        ExternalSystemEntity existing = systemRepo.findById(id)
                .orElseThrow(() -> new BizException(4040, "error.notFound"));
        if (sys.getNameZh() != null) existing.setNameZh(sys.getNameZh());
        if (sys.getNameEn() != null) existing.setNameEn(sys.getNameEn());
        if (sys.getBaseUrl() != null) existing.setBaseUrl(sys.getBaseUrl());
        if (sys.getAuthType() != null) existing.setAuthType(sys.getAuthType());
        if (sys.getAuthConfig() != null) existing.setAuthConfig(sys.getAuthConfig());
        if (sys.getSortOrder() != null) existing.setSortOrder(sys.getSortOrder());
        if (sys.getStatus() != null) existing.setStatus(sys.getStatus());
        return systemRepo.save(existing);
    }

    @Transactional
    public void deleteSystem(Long id) {
        ExternalSystemEntity sys = systemRepo.findById(id)
                .orElseThrow(() -> new BizException(4040, "error.notFound"));
        long count = defRepo.countBySystemCode(sys.getCode());
        if (count > 0)
            throw new BizException(4009, "error.inUse");
        systemRepo.delete(sys);
    }

    // ===== Interface Definition =====

    public List<InterfaceDefinition> listDefinitions() {
        return defRepo.findAllByOrderBySortOrderAsc();
    }

    public List<InterfaceDefinition> listByCategory(String categoryCode) {
        return defRepo.findByCategoryCodeOrderBySortOrderAsc(categoryCode);
    }

    public List<InterfaceDefinition> listBySystem(String systemCode) {
        return defRepo.findBySystemCodeOrderBySortOrderAsc(systemCode);
    }

    @Transactional
    public InterfaceDefinition createDefinition(InterfaceDefinition def) {
        if (def.getCode() == null || def.getCode().trim().isEmpty())
            throw new BizException(4001, "error.validation");
        if (def.getCategoryCode() == null || def.getSystemCode() == null)
            throw new BizException(4001, "error.validation");
        if (defRepo.existsByCode(def.getCode()))
            throw new BizException(4002, "error.duplicate");
        return defRepo.save(def);
    }

    @Transactional
    public InterfaceDefinition updateDefinition(Long id, InterfaceDefinition def) {
        InterfaceDefinition existing = defRepo.findById(id)
                .orElseThrow(() -> new BizException(4040, "error.notFound"));
        if (def.getCategoryCode() != null) existing.setCategoryCode(def.getCategoryCode());
        if (def.getSystemCode() != null) existing.setSystemCode(def.getSystemCode());
        if (def.getNameZh() != null) existing.setNameZh(def.getNameZh());
        if (def.getNameEn() != null) existing.setNameEn(def.getNameEn());
        if (def.getMethod() != null) existing.setMethod(def.getMethod());
        if (def.getPath() != null) existing.setPath(def.getPath());
        if (def.getRequestTemplate() != null) existing.setRequestTemplate(def.getRequestTemplate());
        if (def.getResponseMapping() != null) existing.setResponseMapping(def.getResponseMapping());
        if (def.getSyncDirection() != null) existing.setSyncDirection(def.getSyncDirection());
        if (def.getScheduleCron() != null) existing.setScheduleCron(def.getScheduleCron());
        if (def.getDescription() != null) existing.setDescription(def.getDescription());
        if (def.getSortOrder() != null) existing.setSortOrder(def.getSortOrder());
        if (def.getStatus() != null) existing.setStatus(def.getStatus());
        return defRepo.save(existing);
    }

    @Transactional
    public void deleteDefinition(Long id) {
        InterfaceDefinition def = defRepo.findById(id)
                .orElseThrow(() -> new BizException(4040, "error.notFound"));
        defRepo.delete(def);
    }
}
