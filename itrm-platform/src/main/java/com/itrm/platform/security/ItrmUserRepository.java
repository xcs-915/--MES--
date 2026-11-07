package com.itrm.platform.security;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ItrmUserRepository extends JpaRepository<ItrmUser, Long> {
    Optional<ItrmUser> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<ItrmUser> findFirstByRealName(String realName);
    List<ItrmUser> findByRoleAndStatus(String role, String status);
}
