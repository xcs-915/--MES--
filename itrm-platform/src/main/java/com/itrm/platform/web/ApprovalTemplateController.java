package com.itrm.platform.web;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.domain.ApprovalNode;
import com.itrm.platform.domain.ApprovalTemplate;
import com.itrm.platform.service.ApprovalTemplateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval-templates")
public class ApprovalTemplateController {

    private final ApprovalTemplateService approvalTemplateService;

    public ApprovalTemplateController(ApprovalTemplateService approvalTemplateService) {
        this.approvalTemplateService = approvalTemplateService;
    }

    @GetMapping
    public ApiResponse<List<ApprovalTemplate>> list() {
        return ApiResponse.ok(approvalTemplateService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ApprovalTemplate> get(@PathVariable Long id) {
        return ApiResponse.ok(approvalTemplateService.get(id));
    }

    @PostMapping
    public ApiResponse<ApprovalTemplate> create(@RequestBody Map<String, Object> body) {
        ApprovalTemplate template = new ApprovalTemplate();
        template.setName((String) body.get("name"));
        template.setCode((String) body.get("code"));
        template.setDescription((String) body.get("description"));
        template.setCategory((String) body.get("category"));
        template.setBelongSystem((String) body.get("belongSystem"));
        if (body.get("isDefault") != null) {
            template.setIsDefault(Boolean.valueOf(body.get("isDefault").toString()));
        }
        if (body.get("status") != null) {
            template.setStatus((String) body.get("status"));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodeMaps = (List<Map<String, Object>>) body.get("nodes");
        List<ApprovalNode> nodes = new java.util.ArrayList<>();
        if (nodeMaps != null) {
            for (Map<String, Object> nm : nodeMaps) {
                ApprovalNode node = new ApprovalNode();
                node.setNodeName((String) nm.get("nodeName"));
                node.setNodeType((String) nm.get("nodeType"));
                node.setApproverRole((String) nm.get("approverRole"));
                node.setApproverUser((String) nm.get("approverUser"));
                if (nm.get("sortOrder") != null) {
                    node.setSortOrder(Integer.valueOf(nm.get("sortOrder").toString()));
                }
                nodes.add(node);
            }
        }

        return ApiResponse.ok(approvalTemplateService.create(template, nodes));
    }

    @PutMapping("/{id}")
    public ApiResponse<ApprovalTemplate> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ApprovalTemplate template = new ApprovalTemplate();
        template.setName((String) body.get("name"));
        template.setDescription((String) body.get("description"));
        template.setCategory((String) body.get("category"));
        template.setBelongSystem((String) body.get("belongSystem"));
        if (body.get("isDefault") != null) {
            template.setIsDefault(Boolean.valueOf(body.get("isDefault").toString()));
        }
        if (body.get("status") != null) {
            template.setStatus((String) body.get("status"));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodeMaps = (List<Map<String, Object>>) body.get("nodes");
        List<ApprovalNode> nodes = new java.util.ArrayList<>();
        if (nodeMaps != null) {
            for (Map<String, Object> nm : nodeMaps) {
                ApprovalNode node = new ApprovalNode();
                node.setNodeName((String) nm.get("nodeName"));
                node.setNodeType((String) nm.get("nodeType"));
                node.setApproverRole((String) nm.get("approverRole"));
                node.setApproverUser((String) nm.get("approverUser"));
                if (nm.get("sortOrder") != null) {
                    node.setSortOrder(Integer.valueOf(nm.get("sortOrder").toString()));
                }
                nodes.add(node);
            }
        }

        return ApiResponse.ok(approvalTemplateService.update(id, template, nodes));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        approvalTemplateService.delete(id);
        return ApiResponse.ok();
    }
}
