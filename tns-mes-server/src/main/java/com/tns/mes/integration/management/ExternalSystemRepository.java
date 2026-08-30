package com.tns.mes.integration.management;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalSystemRepository extends JpaRepository<ExternalSystemEntity, Long> {
    Optional<ExternalSystemEntity> findByCode(String code);
    List<ExternalSystemEntity> findAllByOrderBySortOrderAsc();
    boolean existsByCode(String code);
}
