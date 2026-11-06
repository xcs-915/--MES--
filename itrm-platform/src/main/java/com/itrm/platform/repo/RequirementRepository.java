package com.itrm.platform.repo;

import com.itrm.platform.domain.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RequirementRepository extends JpaRepository<Requirement, Long>, JpaSpecificationExecutor<Requirement> {

    boolean existsByReqNo(String reqNo);

    Requirement findByReqNo(String reqNo);

    List<Requirement> findByStatus(String status);

    List<Requirement> findByDevOwner(String devOwner);

    List<Requirement> findBySourceDept(String sourceDept);

    List<Requirement> findByReviewOwner(String reviewOwner);

    List<Requirement> findByProposer(String proposer);
}
