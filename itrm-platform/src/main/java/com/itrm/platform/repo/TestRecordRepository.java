package com.itrm.platform.repo;

import com.itrm.platform.domain.TestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TestRecordRepository extends JpaRepository<TestRecord, Long>, JpaSpecificationExecutor<TestRecord> {

    List<TestRecord> findByRequirementIdOrderByCreatedAtDesc(Long requirementId);

    List<TestRecord> findByTester(String tester);
}
