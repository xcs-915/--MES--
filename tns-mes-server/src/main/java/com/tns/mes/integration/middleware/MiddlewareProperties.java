package com.tns.mes.integration.middleware;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SAP 集成中台连接配置（通道 B）。
 * 老 MES 通过 http://121.41.173.27:9999 调用中台完成领料/报工/入库/退料，
 * 突尼斯新 MES 切换为内网地址 http://10.30.10.42:9999，Token 账号 MES-JX。
 */
@Component
@ConfigurationProperties(prefix = "mes.middleware")
public class MiddlewareProperties {
    /** 是否启用中台集成（关闭后所有中台调用快速失败） */
    private boolean enabled = true;
    /** 中台基础地址 */
    private String baseUrl = "http://10.30.10.42:9999";
    /** Token 账号（老 MES 使用 MES-JX） */
    private String username = "MES-JX";
    /** Token 密码（生产环境通过 K8s Secret 注入） */
    private String password;
    /** Token 本地缓存时长（分钟）；中台 JWT 实际有效期约 5 小时，默认提前 1/3 刷新 */
    private int tokenTtlMinutes = 180;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String value) { baseUrl = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public int getTokenTtlMinutes() { return tokenTtlMinutes; }
    public void setTokenTtlMinutes(int value) { tokenTtlMinutes = value; }
}
