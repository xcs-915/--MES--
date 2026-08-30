package com.tns.mes.quality.service;

import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.quality.domain.Batch;
import com.tns.mes.quality.repo.BatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchService {
    private final BatchRepository repository;

    public BatchService(BatchRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public PageResponse<Batch> page(String keyword, String batchStatus, String plant, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "sapChangedAt", "id"));
        Specification<Batch> spec = Specification.where(null);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String term = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("batchNo")), term),
                    cb.like(cb.lower(root.get("productCode")), term),
                    cb.like(cb.lower(root.get("productName")), term),
                    cb.like(cb.lower(root.get("supplierBatch")), term)));
        }
        if (batchStatus != null && !batchStatus.trim().isEmpty())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("batchStatus"), batchStatus.trim().toUpperCase()));
        if (plant != null && !plant.trim().isEmpty())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("plant"), plant.trim()));
        Page<Batch> result = repository.findAll(spec, pageable);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public Batch get(Long id) { return repository.findById(id).orElseThrow(() -> new BizException(4041, "error.not-found")); }

    @Transactional(readOnly = true)
    public Batch findByBatchNo(String batchNo) {
        return repository.findByBatchNo(batchNo).orElseThrow(() -> new BizException(4041, "error.not-found"));
    }
}
