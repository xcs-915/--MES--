package com.tns.mes.integration.sap.repository;

import com.tns.mes.integration.sap.domain.ApiCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long>, JpaSpecificationExecutor<ApiCallLog> {

    Page<ApiCallLog> findByEndpointContainingIgnoreCase(String endpoint, Pageable pageable);

    Page<ApiCallLog> findBySystemCode(String systemCode, Pageable pageable);

    Page<ApiCallLog> findBySystemCodeAndEndpointContainingIgnoreCase(String systemCode, String endpoint, Pageable pageable);
}
