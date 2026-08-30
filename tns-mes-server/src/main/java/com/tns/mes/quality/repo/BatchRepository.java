package com.tns.mes.quality.repo;

import com.tns.mes.quality.domain.Batch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long>, JpaSpecificationExecutor<Batch> {
    Optional<Batch> findByBatchNoAndPlant(String batchNo, String plant);
    Optional<Batch> findByBatchNo(String batchNo);
    Page<Batch> findByBatchNoContainingIgnoreCaseOrProductCodeContainingIgnoreCase(String batchNo, String productCode, Pageable pageable);
}
