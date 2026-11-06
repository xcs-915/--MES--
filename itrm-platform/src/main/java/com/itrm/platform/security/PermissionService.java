package com.itrm.platform.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {
    private final ItrmUserRepository userRepository;
    private final ItrmRoleRepository roleRepository;

    public PermissionService(ItrmUserRepository userRepository, ItrmRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public Set<String> permissionsFor(String username) {
        ItrmUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !"ACTIVE".equals(user.getStatus())) return Collections.emptySet();
        ItrmRole role = roleRepository.findByCode(user.getRole()).orElse(null);
        if (role == null || !"ACTIVE".equals(role.getStatus())) return Collections.emptySet();
        return role.getPermissions().stream().map(ItrmPermission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
