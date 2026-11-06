package com.itrm.platform.security;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.common.BizException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class RoleAdminController {
    private final ItrmRoleRepository roleRepository;
    private final ItrmPermissionRepository permissionRepository;

    public RoleAdminController(ItrmRoleRepository roleRepository, ItrmPermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('role:view','user:view')")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> roles() {
        return ApiResponse.ok(roleRepository.findAllByOrderByBuiltInDescNameAsc().stream()
                .map(this::toView).collect(Collectors.toList()));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role:view')")
    public ApiResponse<List<Map<String, Object>>> permissions() {
        return ApiResponse.ok(permissionRepository.findAllByOrderBySortOrderAsc().stream().map(p -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId()); row.put("code", p.getCode()); row.put("name", p.getName());
            row.put("module", p.getModule()); row.put("action", p.getAction());
            return row;
        }).collect(Collectors.toList()));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('role:manage')")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String code = text(body, "code").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        String name = text(body, "name");
        if (code.isEmpty() || name.isEmpty()) throw new BizException(400, "角色编码和名称不能为空");
        if (roleRepository.existsByCode(code)) throw new BizException(409, "角色编码已存在");
        ItrmRole role = new ItrmRole();
        role.setCode(code); role.setName(name); role.setDescription(text(body, "description"));
        role.setStatus("ACTIVE"); role.setBuiltIn(false);
        return ApiResponse.ok(toView(roleRepository.save(role)));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ItrmRole role = get(id);
        if (body.containsKey("name")) role.setName(text(body, "name"));
        if (body.containsKey("description")) role.setDescription(text(body, "description"));
        if (body.containsKey("status")) {
            String status = text(body, "status");
            if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) throw new BizException(400, "无效状态");
            if (Boolean.TRUE.equals(role.getBuiltIn()) && "DISABLED".equals(status)) throw new BizException(400, "内置角色不可停用");
            role.setStatus(status);
        }
        return ApiResponse.ok(toView(roleRepository.save(role)));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('role:manage')")
    @Transactional
    public ApiResponse<Map<String, Object>> permissions(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ItrmRole role = get(id);
        Object raw = body.get("permissions");
        List<String> codes = raw instanceof Collection
                ? ((Collection<?>) raw).stream().map(Object::toString).collect(Collectors.toList())
                : Collections.emptyList();
        List<ItrmPermission> found = permissionRepository.findByCodeIn(codes);
        if (found.size() != new HashSet<>(codes).size()) throw new BizException(400, "包含无效权限编码");
        role.setPermissions(new LinkedHashSet<>(found));
        return ApiResponse.ok(toView(roleRepository.save(role)));
    }

    private ItrmRole get(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new BizException(404, "角色不存在"));
    }

    private Map<String, Object> toView(ItrmRole role) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", role.getId()); row.put("code", role.getCode()); row.put("name", role.getName());
        row.put("description", role.getDescription()); row.put("status", role.getStatus()); row.put("builtIn", role.getBuiltIn());
        row.put("permissions", role.getPermissions().stream().map(ItrmPermission::getCode).collect(Collectors.toList()));
        return row;
    }

    private static String text(Map<String, Object> body, String key) {
        Object value = body.get(key); return value == null ? "" : value.toString().trim();
    }
}
