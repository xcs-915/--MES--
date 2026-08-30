package com.tns.mes.integration.management;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterfaceCategoryRepository extends JpaRepository<InterfaceCategory, Long> {
    Optional<InterfaceCategory> findByCode(String code);
    List<InterfaceCategory> findAllByOrderBySortOrderAsc();
    boolean existsByCode(String code);
}
