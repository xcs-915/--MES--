package com.itrm.platform.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ItrmPermissionRepository extends JpaRepository<ItrmPermission, Long> {
    List<ItrmPermission> findAllByOrderBySortOrderAsc();
    List<ItrmPermission> findByCodeIn(Collection<String> codes);
}
