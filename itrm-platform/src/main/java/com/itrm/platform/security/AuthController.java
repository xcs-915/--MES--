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

    public AuthController(ItrmUserRepository userRepository, JwtService jwtService,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
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
        result.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "realName", user.getRealName() != null ? user.getRealName() : user.getUsername(),
                "role", user.getRole(),
                "department", user.getDepartment() != null ? user.getDepartment() : ""
        ));
        return ApiResponse.ok(result);
    }
}
