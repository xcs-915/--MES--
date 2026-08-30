package com.tns.mes.integration.sync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {
    Optional<SyncJob> findByCode(String code);
    List<SyncJob> findByEnabledTrueAndNextRunAtLessThanEqualOrderBySortOrder(LocalDateTime now);
}
