package com.itrm.platform.security;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.common.BizException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ItrmUserRepository userRepository;
    private final JwtService jwtService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    public AuthController(ItrmUserRepository userRepository, JwtService jwtService,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          PermissionService permissionService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.permissionService = permissionService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) throw new BizException(400, "用户名和密码不能为空");

        ItrmUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));

        if (!"ACTIVE".equals(user.getStatus())) throw new BizException(403, "账号已停用");

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new BizException(401, "用户名或密码错误");

        String token = jwtService.generateToken(user.getUsername(), user.getRole(), user.getRealName());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        Map<String, Object> userView = new HashMap<>();
        userView.put("id", user.getId());
        userView.put("username", user.getUsername());
        userView.put("realName", user.getRealName() != null ? user.getRealName() : user.getUsername());
        userView.put("role", user.getRole());
        userView.put("department", user.getDepartment() != null ? user.getDepartment() : "");
        userView.put("permissions", permissionService.permissionsFor(user.getUsername()));
        result.put("user", userView);
        return ApiResponse.ok(result);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(org.springframework.security.core.Authentication authentication) {
        ItrmUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        Map<String, Object> view = new HashMap<>();
        view.put("id", user.getId()); view.put("username", user.getUsername());
        view.put("realName", user.getRealName() != null ? user.getRealName() : user.getUsername());
        view.put("role", user.getRole()); view.put("department", user.getDepartment());
        view.put("permissions", permissionService.permissionsFor(user.getUsername()));
        return ApiResponse.ok(view);
    }
}
