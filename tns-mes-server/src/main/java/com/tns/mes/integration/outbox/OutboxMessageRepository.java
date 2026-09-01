package com.tns.mes.integration.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    /**
     * 查找待发布的 PENDING 消息（next_attempt_at <= now），按创建时间排序。
     * 用于 OutboxPublisher 轮询。
     */
    List<OutboxMessage> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAt(
            String status, Instant now, Pageable pageable);

    /**
     * SQL Server 原子抢占查询：使用 WITH (UPDLOCK, READPAST) 行级锁。
     * - UPDLOCK: 对选中行加更新锁，防止其他事务同时修改
     * - READPAST: 跳过被锁定的行，避免阻塞
     *
     * 在双 Pod 并发环境下，此查询确保同一批消息不会被两个 Pod 同时获取。
     * 调用方在获取后应立即将状态改为 PUBLISHING（同一事务内）以正式占有。
     */
    @Query(value = "SELECT TOP :batchSize * FROM mes_outbox_message " +
            "WITH (UPDLOCK, READPAST) " +
            "WHERE status = 'PENDING' AND next_attempt_at <= :now " +
            "ORDER BY created_at", nativeQuery = true)
    List<OutboxMessage> claimPendingForUpdate(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize);

    /**
     * 统计各状态消息数量（监控用）。
     */
    long countByStatus(String status);
}
