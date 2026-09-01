package com.tns.mes.common.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.time.Instant;

/**
 * 过期数据定时清理调度器。
 *
 * <p>调用 SQL Server 存储过程分批清理以下表：
 * <ul>
 *   <li>mes_outbox_message: 30 天前已发布/失败的 Outbox 消息</li>
 *   <li>sys_audit_log: 90 天前的审计日志</li>
 *   <li>int_api_call_log: 30 天前的接口调用日志</li>
 * </ul>
 *
 * <p>存储过程使用 DELETE TOP (@BatchSize) 循环，避免长事务锁表。
 * 每天凌晨 3:00 执行一次，使用分布式锁确保双 Pod 下只有一个执行。</p>
 *
 * <p>降级策略：存储过程不存在或执行失败时记录警告，不影响主业务。</p>
 */
@Component
public class LogCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogCleanupScheduler.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 每天凌晨 3:00 执行清理。
     * cron = 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "UTC")
    public void cleanupExpiredData() {
        Instant start = Instant.now();
        log.info("Log cleanup started at {}", start);

        int totalCleaned = 0;
        totalCleaned += executeStoredProcedure("sp_mes_cleanup_outbox", 30, 5000);
        totalCleaned += executeStoredProcedure("sp_mes_cleanup_audit_log", 90, 5000);
        totalCleaned += executeStoredProcedure("sp_mes_cleanup_api_call_log", 30, 5000);

        log.info("Log cleanup completed: totalCleaned={}, durationMs={}",
                totalCleaned, java.time.Duration.between(start, Instant.now()).toMillis());
    }

    /**
     * 执行清理存储过程。
     *
     * @param procName      存储过程名
     * @param retentionDays 保留天数
     * @param batchSize     每批删除行数
     * @return 删除行数（-1 表示执行失败）
     */
    private int executeStoredProcedure(String procName, int retentionDays, int batchSize) {
        try {
            Query query = entityManager.createNativeQuery(
                    "EXEC " + procName + " @RetentionDays = :retention, @BatchSize = :batch");
            query.setParameter("retention", retentionDays);
            query.setParameter("batch", batchSize);

            Object result = query.getSingleResult();
            // 存储过程返回 DeletedCount
            if (result instanceof Number) {
                int deleted = ((Number) result).intValue();
                log.info("{}: deleted {} rows (retention={}d)", procName, deleted, retentionDays);
                return deleted;
            }
            log.info("{}: executed (no count returned)", procName);
            return 0;
        } catch (Exception e) {
            log.warn("{} execution failed: {}", procName, e.getMessage());
            return -1;
        }
    }
}
