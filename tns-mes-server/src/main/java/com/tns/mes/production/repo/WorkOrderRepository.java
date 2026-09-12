package com.tns.mes.production.repo;

import com.tns.mes.production.domain.WorkOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {
    boolean existsByOrderNo(String orderNo);
    Optional<WorkOrder> findByOrderNo(String orderNo);
    @EntityGraph(attributePaths = {"product", "bom", "route", "operations", "operations.operation"})
    Optional<WorkOrder> findWithRelationsById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkOrder w where w.id = ?1")
    Optional<WorkOrder> findLockedById(Long id);
    @EntityGraph(attributePaths = {"product", "bom", "route", "operations", "operations.operation"})
    Page<WorkOrder> findByStatus(String status, Pageable pageable);
    @EntityGraph(attributePaths = {"product", "bom", "route", "operations", "operations.operation"})
    Page<WorkOrder> findAll(Pageable pageable);
    @Override
    @EntityGraph(attributePaths = {"product", "bom", "route"})
    Page<WorkOrder> findAll(Specification<WorkOrder> spec, Pageable pageable);
}
