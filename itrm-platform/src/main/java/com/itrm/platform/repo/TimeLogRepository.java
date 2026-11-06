package com.itrm.platform.repo;

import com.itrm.platform.domain.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long>, JpaSpecificationExecutor<TimeLog> {

    List<TimeLog> findByRequirementIdOrderByWorkDateDesc(Long requirementId);

    List<TimeLog> findByWorkerOrderByWorkDateDesc(String worker);

    List<TimeLog> findByTaskId(Long taskId);

    boolean existsByTaskId(Long taskId);
}
