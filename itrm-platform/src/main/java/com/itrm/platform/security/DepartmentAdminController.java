package com.itrm.platform.security;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.common.BizException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/departments")
public class DepartmentAdminController {
    private final ItrmDepartmentRepository departmentRepository;

    public DepartmentAdminController(ItrmDepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(departmentRepository.findAllByOrderByFullPathAsc().stream()
                .map(this::toView).collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String name = text(body.get("name"));
        String parentPath = normalizePath(text(body.get("parentPath")));
        if (name.isEmpty() || name.contains("/") || name.contains(";")) {
            throw new BizException(400, "部门名称不能为空，且不能包含 / 或分号");
        }
        if (!parentPath.isEmpty() && !departmentRepository.existsByFullPath(parentPath)) {
            throw new BizException(400, "上级部门不存在");
        }
        String fullPath = parentPath.isEmpty() ? name : parentPath + "/" + name;
        if (fullPath.length() > 200) throw new BizException(400, "部门路径不能超过 200 个字符");
        if (departmentRepository.existsByFullPath(fullPath)) throw new BizException(409, "部门已存在");
        ItrmDepartment department = new ItrmDepartment();
        department.setName(name);
        department.setParentPath(parentPath.isEmpty() ? null : parentPath);
        department.setFullPath(fullPath);
        department.setStatus("ACTIVE");
        return ApiResponse.ok(toView(departmentRepository.save(department)));
    }

    private Map<String, Object> toView(ItrmDepartment department) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", department.getId());
        row.put("name", department.getName());
        row.put("fullPath", department.getFullPath());
        row.put("parentPath", department.getParentPath());
        row.put("status", department.getStatus());
        return row;
    }

    static String normalizePath(String value) {
        return value.replace('\\', '/').replaceAll("^/+|/+$", "").replaceAll("/{2,}", "/");
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
