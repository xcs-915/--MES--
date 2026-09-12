package com.tns.mes.execution.repo;

import com.tns.mes.execution.domain.WorkReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import javax.persistence.LockModeType;

import java.util.Optional;

public interface WorkReportRepository extends JpaRepository<WorkReport, Long>, JpaSpecificationExecutor<WorkReport> {
    @Override
    @EntityGraph(attributePaths = {"workOrder", "workOrder.product", "operation"})
    Page<WorkReport> findAll(org.springframework.data.jpa.domain.Specification<WorkReport> spec, Pageable pageable);
    @EntityGraph(attributePaths = {"workOrder", "workOrder.product", "operation"})
    Optional<WorkReport> findWithRelationsById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from WorkReport r where r.id = ?1")
    Optional<WorkReport> findLockedById(Long id);
}
