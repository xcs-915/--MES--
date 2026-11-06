package com.itrm.platform.repo;

import com.itrm.platform.domain.ApprovalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long>, JpaSpecificationExecutor<ApprovalRecord> {

    List<ApprovalRecord> findByRequirementIdOrderByCreatedAtDesc(Long requirementId);
}
