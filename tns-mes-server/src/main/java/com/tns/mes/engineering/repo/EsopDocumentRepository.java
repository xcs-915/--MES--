package com.tns.mes.engineering.repo;

import com.tns.mes.engineering.domain.EsopDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EsopDocumentRepository extends JpaRepository<EsopDocument, Long>, JpaSpecificationExecutor<EsopDocument> {
    boolean existsByDocumentNoAndVersionCode(String documentNo, String versionCode);
    List<EsopDocument> findByDocumentNoAndStatus(String documentNo, String status);
}
