package com.tns.mes.execution.repo;

import com.tns.mes.execution.domain.GoodsReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import javax.persistence.LockModeType;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long>, JpaSpecificationExecutor<GoodsReceipt> {
    @Override
    @EntityGraph(attributePaths = {"workOrder", "workOrder.product", "workReport", "inspection"})
    Page<GoodsReceipt> findAll(org.springframework.data.jpa.domain.Specification<GoodsReceipt> spec, Pageable pageable);
    @EntityGraph(attributePaths = {"workOrder", "workOrder.product", "workReport", "inspection"})
    Optional<GoodsReceipt> findWithRelationsById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GoodsReceipt g where g.id = ?1")
    Optional<GoodsReceipt> findLockedById(Long id);
    @Query("select coalesce(sum(g.quantity), 0) from GoodsReceipt g where g.workOrder.id = ?1 and g.status in ('POSTING','POSTED')")
    BigDecimal sumPostedByWorkOrderId(Long workOrderId);
    @Query("select coalesce(sum(g.quantity), 0) from GoodsReceipt g where g.inspection.id = ?1 and g.status in ('POSTING','POSTED')")
    BigDecimal sumPostedByInspectionId(Long inspectionId);
    @Query("select coalesce(sum(g.quantity), 0) from GoodsReceipt g where g.workReport.id = ?1 and g.status in ('POSTING','POSTED')")
    BigDecimal sumPostedByWorkReportId(Long workReportId);
}
