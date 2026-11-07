package com.itrm.platform.web;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.service.RequirementImportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/requirement-imports")
public class RequirementImportController {
    private final RequirementImportService importService;
    public RequirementImportController(RequirementImportService importService) { this.importService = importService; }

    @PostMapping
    @PreAuthorize("hasAuthority('requirement:create')")
    public ApiResponse<Map<String, Object>> importRequirements(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(importService.importRequirements(body));
    }
}
