package com.itrm.platform.repo;

import com.itrm.platform.domain.ApprovalNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ApprovalNodeRepository extends JpaRepository<ApprovalNode, Long>, JpaSpecificationExecutor<ApprovalNode> {

    List<ApprovalNode> findByTemplateIdOrderBySortOrderAsc(Long templateId);

    void deleteByTemplateId(Long templateId);
}
