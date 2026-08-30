package com.tns.mes.model;

import com.google.gson.annotations.SerializedName;

/**
 * 登录响应：POST /api/v1/auth/login 返回 { "token": "...", "user": {...} }
 *
 * 注意：若后端实际使用统一返回体（如 { "code":200, "msg":"...", "token":"...", "user":{...} }），
 *      token/user 字段依然可被 Gson 正确解析；额外的 code/msg 字段会被忽略。
 *      isSuccessful() 以 token 非空作为登录成功的判定依据。
 */
public class LoginResponse {

    @SerializedName("token")
    private String token;

    @SerializedName("user")
    private User user;

    /** 兼容统一返回体的可选字段 */
    @SerializedName("code")
    private Integer code;

    @SerializedName("msg")
    private String msg;

    public boolean isSuccessful() {
        return token != null && !token.trim().isEmpty();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    /**
     * 登录用户信息
     */
    public static class User {

        @SerializedName("id")
        private Long id;

        @SerializedName("username")
        private String username;

        @SerializedName("realName")
        private String realName;

        @SerializedName("nickName")
        private String nickName;

        @SerializedName("role")
        private String role;

        @SerializedName("roles")
        private java.util.List<String> roles;

        @SerializedName("deptName")
        private String deptName;

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            if (realName != null && !realName.isEmpty()) {
                return realName;
            }
            if (nickName != null && !nickName.isEmpty()) {
                return nickName;
            }
            return username;
        }

        public String getRole() {
            return role;
        }
    }
}
