package com.itrm.platform.security;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus,Long>{
    List<WorkflowStatus> findAllByOrderBySortOrderAsc();
    Optional<WorkflowStatus> findByCode(String code);
    boolean existsByCode(String code);
}
