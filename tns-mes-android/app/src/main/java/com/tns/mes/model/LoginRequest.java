package com.tns.mes.model;

import com.google.gson.annotations.SerializedName;

/**
 * 请求体：POST /api/v1/auth/login
 * 对应 JSON：{ "username": "...", "password": "..." }
 */
public class LoginRequest {

    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
