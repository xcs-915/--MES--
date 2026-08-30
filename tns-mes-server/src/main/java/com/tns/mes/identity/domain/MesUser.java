package com.tns.mes.identity.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "iam_user")
public class MesUser extends AuditedEntity {
    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    @Column(length = 200)
    private String email;
    @Column(name = "language_code", nullable = false, length = 16)
    private String languageCode = "zh-CN";
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "iam_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<MesRole> roles = new HashSet<>();

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Set<MesRole> getRoles() { return roles; }
    public void setRoles(Set<MesRole> roles) { this.roles = roles; }
}

