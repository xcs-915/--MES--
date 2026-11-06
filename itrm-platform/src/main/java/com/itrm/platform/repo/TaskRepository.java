package com.itrm.platform.repo;

import com.itrm.platform.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByRequirementIdOrderBySortOrderAsc(Long requirementId);

    List<Task> findByAssignee(String assignee);

    long countByRequirementIdAndStatus(Long requirementId, String status);
}
