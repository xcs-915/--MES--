package com.itrm.platform.repo;

import com.itrm.platform.domain.ApprovalTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ApprovalTemplateRepository extends JpaRepository<ApprovalTemplate, Long>, JpaSpecificationExecutor<ApprovalTemplate> {

    List<ApprovalTemplate> findByStatus(String status);

    Optional<ApprovalTemplate> findByIsDefaultTrue();

    ApprovalTemplate findByCode(String code);
}
