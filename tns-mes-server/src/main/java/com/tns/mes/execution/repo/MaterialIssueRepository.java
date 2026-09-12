package com.tns.mes.execution.repo;

import com.tns.mes.execution.domain.MaterialIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import javax.persistence.LockModeType;

import java.util.Optional;

public interface MaterialIssueRepository extends JpaRepository<MaterialIssue, Long>, JpaSpecificationExecutor<MaterialIssue> {
    @Override
    @EntityGraph(attributePaths = {"workOrder", "workOrder.product", "operation", "items"})
    Page<MaterialIssue> findAll(org.springframework.data.jpa.domain.Specification<MaterialIssue> spec, Pageable pageable);
    @EntityGraph(attributePaths = {"workOrder", "workOrder.product", "operation", "items"})
    Optional<MaterialIssue> findWithRelationsById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from MaterialIssue i where i.id = ?1")
    Optional<MaterialIssue> findLockedById(Long id);
}
