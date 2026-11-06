package com.itrm.platform.repo;

import com.itrm.platform.domain.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long>, JpaSpecificationExecutor<ReviewLog> {

    List<ReviewLog> findByRequirementIdOrderByCreatedAtDesc(Long requirementId);
}
