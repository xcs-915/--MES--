package com.tns.mes.integration.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAt(String status, Instant now, Pageable pageable);
}

