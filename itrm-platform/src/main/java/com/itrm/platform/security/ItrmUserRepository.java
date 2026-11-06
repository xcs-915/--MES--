package com.itrm.platform.security;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ItrmUserRepository extends JpaRepository<ItrmUser, Long> {
    Optional<ItrmUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
