package com.itrm.platform.security;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.common.BizException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {
    private final ItrmUserRepository userRepository;
    private final ItrmRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminController(ItrmUserRepository userRepository, ItrmRoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:view')")
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Map<String, Object>> rows = userRepository.findAll().stream()
                .sorted(Comparator.comparing(ItrmUser::getUsername))
                .map(this::toView)
                .collect(Collectors.toList());
        return ApiResponse.ok(rows);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String username = text(body, "username");
        String password = text(body, "password");
        String role = text(body, "role");
        if (username.isEmpty() || password.length() < 6 || role.isEmpty()) {
            throw new BizException(400, "用户名、角色不能为空，密码至少 6 位");
        }
        if (userRepository.existsByUsername(username)) throw new BizException(409, "用户名已存在");
        ensureRole(role);
        ItrmUser user = new ItrmUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        apply(user, body);
        user.setStatus("ACTIVE");
        return ApiResponse.ok(toView(userRepository.save(user)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ItrmUser user = get(id);
        String role = text(body, "role");
        if (!role.isEmpty()) ensureRole(role);
        apply(user, body);
        return ApiResponse.ok(toView(userRepository.save(user)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:disable')")
    public ApiResponse<Map<String, Object>> status(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ItrmUser user = get(id);
        String status = text(body, "status");
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) throw new BizException(400, "无效状态");
        if ("admin".equals(user.getUsername()) && "DISABLED".equals(status)) {
            throw new BizException(400, "默认管理员不可停用");
        }
        user.setStatus(status);
        return ApiResponse.ok(toView(userRepository.save(user)));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('user:reset-password')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String password = text(body, "password");
        if (password.length() < 6) throw new BizException(400, "密码至少 6 位");
        ItrmUser user = get(id);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return ApiResponse.ok();
    }

    private ItrmUser get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BizException(404, "用户不存在"));
    }

    private void ensureRole(String code) {
        ItrmRole role = roleRepository.findByCode(code).orElseThrow(() -> new BizException(400, "角色不存在"));
        if (!"ACTIVE".equals(role.getStatus())) throw new BizException(400, "角色已停用");
    }

    private void apply(ItrmUser user, Map<String, Object> body) {
        if (body.containsKey("realName")) user.setRealName(text(body, "realName"));
        if (body.containsKey("role")) user.setRole(text(body, "role"));
        if (body.containsKey("department")) user.setDepartment(text(body, "department"));
        if (body.containsKey("email")) user.setEmail(text(body, "email"));
    }

    private Map<String, Object> toView(ItrmUser user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("realName", user.getRealName());
        row.put("role", user.getRole());
        row.put("department", user.getDepartment());
        row.put("email", user.getEmail());
        row.put("status", user.getStatus());
        row.put("createdAt", user.getCreatedAt());
        return row;
    }

    private static String text(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : value.toString().trim();
    }
}
