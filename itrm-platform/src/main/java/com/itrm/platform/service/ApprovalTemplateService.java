package com.itrm.platform.service;

import com.itrm.platform.common.BizException;
import com.itrm.platform.domain.ApprovalNode;
import com.itrm.platform.domain.ApprovalTemplate;
import com.itrm.platform.repo.ApprovalNodeRepository;
import com.itrm.platform.repo.ApprovalTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
public class ApprovalTemplateService {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalNodeRepository approvalNodeRepository;

    public ApprovalTemplateService(ApprovalTemplateRepository approvalTemplateRepository,
                                   ApprovalNodeRepository approvalNodeRepository) {
        this.approvalTemplateRepository = approvalTemplateRepository;
        this.approvalNodeRepository = approvalNodeRepository;
    }

    /**
     * 列出所有启用的模板（带节点）
     */
    @Transactional(readOnly = true)
    public List<ApprovalTemplate> list() {
        List<ApprovalTemplate> templates = approvalTemplateRepository.findByStatus("ACTIVE");
        for (ApprovalTemplate template : templates) {
            List<ApprovalNode> nodes = approvalNodeRepository.findByTemplateIdOrderBySortOrderAsc(template.getId());
            template.setNodes(nodes);
        }
        return templates;
    }

    /**
     * 获取模板详情（带节点）
     */
    @Transactional(readOnly = true)
    public ApprovalTemplate get(Long id) {
        ApprovalTemplate template = approvalTemplateRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "审批模板不存在"));
        List<ApprovalNode> nodes = approvalNodeRepository.findByTemplateIdOrderBySortOrderAsc(id);
        template.setNodes(nodes);
        return template;
    }

    /**
     * 创建模板+节点
     */
    @Transactional
    public ApprovalTemplate create(ApprovalTemplate template, List<ApprovalNode> nodes) {
        if (!StringUtils.hasText(template.getName())) {
            throw new BizException(400, "模板名称不能为空");
        }
        if (!StringUtils.hasText(template.getCode())) {
            throw new BizException(400, "模板编码不能为空");
        }
        // 检查编码唯一性
        ApprovalTemplate existing = approvalTemplateRepository.findByCode(template.getCode());
        if (existing != null) {
            throw new BizException(400, "模板编码已存在");
        }

        if (template.getStatus() == null) {
            template.setStatus("ACTIVE");
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(false);
        }

        ApprovalTemplate saved = approvalTemplateRepository.save(template);

        // 保存节点
        if (nodes != null && !nodes.isEmpty()) {
            int sortOrder = 0;
            for (ApprovalNode node : nodes) {
                node.setTemplateId(saved.getId());
                node.setSortOrder(sortOrder++);
                node.setCreatedAt(Instant.now());
                approvalNodeRepository.save(node);
            }
        }

        saved.setNodes(approvalNodeRepository.findByTemplateIdOrderBySortOrderAsc(saved.getId()));
        return saved;
    }

    /**
     * 更新模板
     */
    @Transactional
    public ApprovalTemplate update(Long id, ApprovalTemplate template, List<ApprovalNode> nodes) {
        ApprovalTemplate existing = approvalTemplateRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "审批模板不存在"));

        if (StringUtils.hasText(template.getName())) {
            existing.setName(template.getName());
        }
        if (template.getDescription() != null) {
            existing.setDescription(template.getDescription());
        }
        if (template.getCategory() != null) {
            existing.setCategory(template.getCategory());
        }
        if (template.getBelongSystem() != null) {
            existing.setBelongSystem(template.getBelongSystem());
        }
        if (template.getIsDefault() != null) {
            existing.setIsDefault(template.getIsDefault());
        }
        if (template.getStatus() != null) {
            existing.setStatus(template.getStatus());
        }

        ApprovalTemplate saved = approvalTemplateRepository.save(existing);

        // 更新节点：先删后增
        if (nodes != null) {
            approvalNodeRepository.deleteByTemplateId(id);
            int sortOrder = 0;
            for (ApprovalNode node : nodes) {
                node.setId(null);
                node.setTemplateId(saved.getId());
                node.setSortOrder(sortOrder++);
                node.setCreatedAt(Instant.now());
                approvalNodeRepository.save(node);
            }
        }

        saved.setNodes(approvalNodeRepository.findByTemplateIdOrderBySortOrderAsc(saved.getId()));
        return saved;
    }

    /**
     * 删除模板
     */
    @Transactional
    public void delete(Long id) {
        ApprovalTemplate template = approvalTemplateRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "审批模板不存在"));
        approvalNodeRepository.deleteByTemplateId(id);
        approvalTemplateRepository.delete(template);
    }
}
