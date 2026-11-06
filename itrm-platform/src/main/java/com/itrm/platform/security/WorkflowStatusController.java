package com.itrm.platform.security;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.common.BizException;
import com.itrm.platform.repo.RequirementRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/workflow-statuses")
public class WorkflowStatusController {
    private final WorkflowStatusRepository repository;
    private final RequirementRepository requirementRepository;
    public WorkflowStatusController(WorkflowStatusRepository repository, RequirementRepository requirementRepository){this.repository=repository;this.requirementRepository=requirementRepository;}

    @GetMapping
    public ApiResponse<List<WorkflowStatus>> list(){return ApiResponse.ok(repository.findAllByOrderBySortOrderAsc());}

    @PostMapping @PreAuthorize("hasAuthority('workflow:manage')")
    public ApiResponse<WorkflowStatus> create(@RequestBody Map<String,Object> body){
        String code=text(body,"code").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]","_");
        if(code.isEmpty()||text(body,"name").isEmpty())throw new BizException(400,"状态编码和名称不能为空");
        if(repository.existsByCode(code))throw new BizException(409,"状态编码已存在");
        WorkflowStatus s=new WorkflowStatus();s.setCode(code);apply(s,body);s.setBuiltIn(false);s.setStatus("ACTIVE");return ApiResponse.ok(repository.save(s));
    }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('workflow:manage')")
    public ApiResponse<WorkflowStatus> update(@PathVariable Long id,@RequestBody Map<String,Object> body){WorkflowStatus s=get(id);apply(s,body);return ApiResponse.ok(repository.save(s));}
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('workflow:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id){WorkflowStatus s=get(id);if(Boolean.TRUE.equals(s.getBuiltIn()))throw new BizException(400,"内置状态不可删除");if(!requirementRepository.findByStatus(s.getCode()).isEmpty())throw new BizException(409,"该状态已被需求使用，不可删除");repository.delete(s);return ApiResponse.ok();}
    private WorkflowStatus get(Long id){return repository.findById(id).orElseThrow(()->new BizException(404,"状态不存在"));}
    private void apply(WorkflowStatus s,Map<String,Object>b){if(b.containsKey("name"))s.setName(text(b,"name"));if(b.containsKey("color"))s.setColor(text(b,"color"));if(b.containsKey("sortOrder"))s.setSortOrder(Integer.valueOf(b.get("sortOrder").toString()));if(b.containsKey("terminal"))s.setTerminal(Boolean.valueOf(b.get("terminal").toString()));if(b.containsKey("status"))s.setStatus(text(b,"status"));}
    private static String text(Map<String,Object>b,String k){Object v=b.get(k);return v==null?"":v.toString().trim();}
}
