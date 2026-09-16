package com.tns.mes.engineering.repo;

import com.tns.mes.engineering.domain.EsopReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EsopReviewLogRepository extends JpaRepository<EsopReviewLog, Long> {
    List<EsopReviewLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
