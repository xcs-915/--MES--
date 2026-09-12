package com.tns.mes.execution.repo;

import com.tns.mes.execution.domain.QualityInspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.Collection;

public interface QualityInspectionRepository extends JpaRepository<QualityInspection, Long>, JpaSpecificationExecutor<QualityInspection> {
    @Override
    @EntityGraph(attributePaths = {"workOrder", "workOrder.product", "operation", "workReport", "items"})
    Page<QualityInspection> findAll(org.springframework.data.jpa.domain.Specification<QualityInspection> spec, Pageable pageable);
    @EntityGraph(attributePaths = {"workOrder", "workOrder.product", "operation", "workReport", "items"})
    Optional<QualityInspection> findWithRelationsById(Long id);
    boolean existsByWorkOrder_IdAndInspectionTypeAndStatusIn(Long workOrderId, String inspectionType, Collection<String> statuses);
    boolean existsByWorkOrder_IdAndInspectionType(Long workOrderId, String inspectionType);
    boolean existsByWorkOrder_IdAndInspectionTypeAndStatus(Long workOrderId, String inspectionType, String status);
}
