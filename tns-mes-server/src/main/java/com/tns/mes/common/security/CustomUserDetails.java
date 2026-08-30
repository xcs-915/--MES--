package com.tns.mes.common.security;

import com.tns.mes.identity.domain.MesPermission;
import com.tns.mes.identity.domain.MesRole;
import com.tns.mes.identity.domain.MesUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final MesUser user;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(MesUser user) {
        this.user = user;
        this.authorities = new ArrayList<>();
        for (MesRole role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            for (MesPermission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }
    }

    public MesUser getUser() { return user; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getUsername(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return "ACTIVE".equalsIgnoreCase(user.getStatus()); }
}

