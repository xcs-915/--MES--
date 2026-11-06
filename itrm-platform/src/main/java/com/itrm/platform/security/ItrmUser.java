package com.itrm.platform.security;

import com.itrm.platform.common.BaseEntity;

import javax.persistence.*;

@Entity
@Table(name = "itrm_user")
public class ItrmUser extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(length = 64)
    private String realName;

    @Column(length = 30)
    private String role; // ADMIN / MANAGER / DEVELOPER / TESTER / REQUESTER

    @Column(length = 64)
    private String department;

    @Column(length = 20)
    private String status; // ACTIVE / DISABLED

    @Column(length = 200)
    private String email;

    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { password = v; }
    public String getRealName() { return realName; }
    public void setRealName(String v) { realName = v; }
    public String getRole() { return role; }
    public void setRole(String v) { role = v; }
    public String getDepartment() { return department; }
    public void setDepartment(String v) { department = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
}
