package com.tns.mes.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.tns.mes.model.LoginResponse;

/**
 * 会话管理器：基于 SharedPreferences 持久化存储 JWT Token 与登录用户信息。
 *
 * <p>存储的 SharedPreferences 文件名为 {@value #PREF_NAME}。</p>
 */
public class SessionManager {

    private static final String PREF_NAME = "tns_mes_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_LOGIN_TIME = "login_time";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        // 使用 ApplicationContext 防止内存泄漏
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 保存登录响应中的认证信息。
     */
    public void saveAuth(LoginResponse response, String inputUsername) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TOKEN, response.getToken());

        LoginResponse.User user = response.getUser();
        if (user != null) {
            editor.putLong(KEY_USER_ID, user.getId() != null ? user.getId() : -1L);
            editor.putString(KEY_USERNAME, user.getUsername() != null ? user.getUsername() : inputUsername);
            editor.putString(KEY_DISPLAY_NAME, user.getDisplayName());
            editor.putString(KEY_ROLE, user.getRole());
        } else {
            editor.putString(KEY_USERNAME, inputUsername);
        }

        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * 获取 JWT Token。未登录时返回 null。
     */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /**
     * 构造 HTTP Authorization 头的值，例如 "Bearer xxx.yyy.zzz"。
     * 未登录时返回 null。
     */
    public String getAuthHeader() {
        String token = getToken();
        if (token == null || token.isEmpty()) {
            return null;
        }
        return "Bearer " + token;
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, getUsername());
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "");
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    public long getLoginTime() {
        return prefs.getLong(KEY_LOGIN_TIME, 0L);
    }

    /**
     * 清除登录状态（退出登录）。
     */
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
