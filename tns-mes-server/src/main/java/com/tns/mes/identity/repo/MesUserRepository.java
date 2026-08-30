package com.tns.mes.identity.repo;

import com.tns.mes.identity.domain.MesUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MesUserRepository extends JpaRepository<MesUser, Long> {
    Optional<MesUser> findByUsername(String username);
}

