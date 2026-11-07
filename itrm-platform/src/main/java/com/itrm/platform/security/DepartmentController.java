package com.itrm.platform.security;

import com.itrm.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private final ItrmDepartmentRepository departmentRepository;

    public DepartmentController(ItrmDepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(departmentRepository.findAllByOrderByFullPathAsc().stream()
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", item.getId());
                    row.put("name", item.getName());
                    row.put("fullPath", item.getFullPath());
                    row.put("parentPath", item.getParentPath());
                    row.put("status", item.getStatus());
                    return row;
                }).collect(Collectors.toList()));
    }
}
