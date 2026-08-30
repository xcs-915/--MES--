package com.tns.mes.identity.repo;

import com.tns.mes.identity.domain.MesRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MesRoleRepository extends JpaRepository<MesRole, Long> {
    Optional<MesRole> findByCode(String code);
}

