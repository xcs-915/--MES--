package com.tns.mes.integration.management;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterfaceDefinitionRepository extends JpaRepository<InterfaceDefinition, Long> {
    Optional<InterfaceDefinition> findByCode(String code);
    List<InterfaceDefinition> findByCategoryCodeOrderBySortOrderAsc(String categoryCode);
    List<InterfaceDefinition> findBySystemCodeOrderBySortOrderAsc(String systemCode);
    List<InterfaceDefinition> findAllByOrderBySortOrderAsc();
    boolean existsByCode(String code);
    long countByCategoryCode(String categoryCode);
    long countBySystemCode(String systemCode);
}
