package com.tns.mes.identity.web;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.common.security.CustomUserDetails;
import com.tns.mes.common.security.JwtService;
import com.tns.mes.identity.domain.MesUser;
import com.tns.mes.identity.repo.MesUserRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MesUserRepository users;
    private final MessageSource messages;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          MesUserRepository users, MessageSource messages) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.users = users;
        this.messages = messages;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            CustomUserDetails details = (CustomUserDetails) authentication.getPrincipal();
            if (!details.isEnabled()) {
                throw new BizException(4012, "auth.disabled");
            }
            String token = jwtService.issue(details);
            List<String> permissions = details.getAuthorities().stream().map(a -> a.getAuthority())
                    .filter(a -> !a.startsWith("ROLE_")).collect(Collectors.toList());
            return ApiResponse.ok(new LoginResponse(token, details.getUsername(), details.getUser().getDisplayName(),
                    details.getUser().getLanguageCode(), permissions), requestId(servletRequest));
        } catch (AuthenticationException ex) {
            throw new BizException(4011, "auth.invalid");
        }
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(Authentication authentication, HttpServletRequest request) {
        MesUser user = users.findByUsername(authentication.getName()).orElseThrow(() -> new BizException(4041, "error.not-found"));
        List<String> permissions = new CustomUserDetails(user).getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> !a.startsWith("ROLE_")).collect(Collectors.toList());
        return ApiResponse.ok(new MeResponse(user.getUsername(), user.getDisplayName(), user.getLanguageCode(), permissions), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute("requestId");
        return value == null ? null : value.toString();
    }

    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String accessToken;
        private String tokenType = "Bearer";
        private String username;
        private String displayName;
        private String language;
        private List<String> permissions;
        public LoginResponse(String accessToken, String username, String displayName, String language, List<String> permissions) {
            this.accessToken = accessToken; this.username = username; this.displayName = displayName;
            this.language = language; this.permissions = permissions;
        }
        public String getAccessToken() { return accessToken; }
        public String getTokenType() { return tokenType; }
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public String getLanguage() { return language; }
        public List<String> getPermissions() { return permissions; }
    }

    public static class MeResponse {
        private String username;
        private String displayName;
        private String language;
        private List<String> permissions;
        public MeResponse(String username, String displayName, String language, List<String> permissions) {
            this.username = username; this.displayName = displayName; this.language = language; this.permissions = permissions;
        }
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public String getLanguage() { return language; }
        public List<String> getPermissions() { return permissions; }
    }
}

