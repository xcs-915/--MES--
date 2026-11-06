package com.itrm.platform.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItrmRoleRepository extends JpaRepository<ItrmRole, Long> {
    Optional<ItrmRole> findByCode(String code);
    boolean existsByCode(String code);
    List<ItrmRole> findAllByOrderByBuiltInDescNameAsc();
}
