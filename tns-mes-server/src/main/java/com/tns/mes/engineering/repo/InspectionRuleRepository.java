package com.tns.mes.engineering.repo;

import com.tns.mes.engineering.domain.InspectionRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InspectionRuleRepository extends JpaRepository<InspectionRule, Long> {
    @EntityGraph(attributePaths = "items")
    Optional<InspectionRule> findWithItemsById(Long id);
    @EntityGraph(attributePaths = "items")
    Page<InspectionRule> findAllBy(Pageable pageable);
    boolean existsByCode(String code);
}
