package com.tns.mes.identity.repo;

import com.tns.mes.identity.domain.MesPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MesPermissionRepository extends JpaRepository<MesPermission, Long> {
    Optional<MesPermission> findByCode(String code);
}

