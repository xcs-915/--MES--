package com.tns.mes.integration.management;

import com.tns.mes.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Interface Management Controller
 * Provides CRUD APIs for interface categories, external systems, and interface definitions.
 * Used by the frontend "接口管理" page to manage multi-system integrations.
 */
@RestController
@RequestMapping("/api/v1/interfaces")
public class InterfaceManagementController {

    private final InterfaceManagementService service;

    public InterfaceManagementController(InterfaceManagementService service) {
        this.service = service;
    }

    private String id(HttpServletRequest request) {
        Object value = request.getAttribute("requestId");
        return value == null ? null : value.toString();
    }

    // ===== Category =====

    @GetMapping("/categories")
    public ApiResponse<List<InterfaceCategory>> listCategories(HttpServletRequest request) {
        return ApiResponse.ok(service.listCategories(), id(request));
    }

    @PostMapping("/categories")
    public ApiResponse<InterfaceCategory> createCategory(@RequestBody InterfaceCategory body, HttpServletRequest request) {
        return ApiResponse.ok(service.createCategory(body), id(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<InterfaceCategory> updateCategory(@PathVariable Long id, @RequestBody InterfaceCategory body, HttpServletRequest request) {
        return ApiResponse.ok(service.updateCategory(id, body), id(request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id, HttpServletRequest request) {
        service.deleteCategory(id);
        return ApiResponse.ok(null, id(request));
    }

    // ===== External System =====

    @GetMapping("/systems")
    public ApiResponse<List<ExternalSystemEntity>> listSystems(HttpServletRequest request) {
        return ApiResponse.ok(service.listSystems(), id(request));
    }

    @PostMapping("/systems")
    public ApiResponse<ExternalSystemEntity> createSystem(@RequestBody ExternalSystemEntity body, HttpServletRequest request) {
        return ApiResponse.ok(service.createSystem(body), id(request));
    }

    @PutMapping("/systems/{id}")
    public ApiResponse<ExternalSystemEntity> updateSystem(@PathVariable Long id, @RequestBody ExternalSystemEntity body, HttpServletRequest request) {
        return ApiResponse.ok(service.updateSystem(id, body), id(request));
    }

    @DeleteMapping("/systems/{id}")
    public ApiResponse<Void> deleteSystem(@PathVariable Long id, HttpServletRequest request) {
        service.deleteSystem(id);
        return ApiResponse.ok(null, id(request));
    }

    // ===== Interface Definition =====

    @GetMapping("/definitions")
    public ApiResponse<List<InterfaceDefinition>> listDefinitions(
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String systemCode,
            HttpServletRequest request) {
        if (categoryCode != null && !categoryCode.isEmpty()) {
            return ApiResponse.ok(service.listByCategory(categoryCode), id(request));
        }
        if (systemCode != null && !systemCode.isEmpty()) {
            return ApiResponse.ok(service.listBySystem(systemCode), id(request));
        }
        return ApiResponse.ok(service.listDefinitions(), id(request));
    }

    @PostMapping("/definitions")
    public ApiResponse<InterfaceDefinition> createDefinition(@RequestBody InterfaceDefinition body, HttpServletRequest request) {
        return ApiResponse.ok(service.createDefinition(body), id(request));
    }

    @PutMapping("/definitions/{id}")
    public ApiResponse<InterfaceDefinition> updateDefinition(@PathVariable Long id, @RequestBody InterfaceDefinition body, HttpServletRequest request) {
        return ApiResponse.ok(service.updateDefinition(id, body), id(request));
    }

    @DeleteMapping("/definitions/{id}")
    public ApiResponse<Void> deleteDefinition(@PathVariable Long id, HttpServletRequest request) {
        service.deleteDefinition(id);
        return ApiResponse.ok(null, id(request));
    }
}
