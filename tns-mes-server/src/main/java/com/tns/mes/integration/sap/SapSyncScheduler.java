package com.tns.mes.integration.sap;

import com.tns.mes.integration.sync.SyncJob;
import com.tns.mes.integration.sync.SyncJobRepository;
import com.tns.mes.integration.sync.SyncJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SapSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(SapSyncScheduler.class);
    private final SapProperties properties;
    private final SyncJobRepository jobs;
    private final SyncJobService service;
    private final AtomicBoolean polling = new AtomicBoolean();

    public SapSyncScheduler(SapProperties properties, SyncJobRepository jobs, SyncJobService service) {
        this.properties = properties;
        this.jobs = jobs;
        this.service = service;
    }

    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    public void runDueJobs() {
        if (!properties.isEnabled() || !polling.compareAndSet(false, true)) return;
        try {
            for (SyncJob job : jobs.findByEnabledTrueAndNextRunAtLessThanEqualOrderBySortOrder(LocalDateTime.now())) {
                try {
                    log.info("Starting scheduled integration job {}", job.getCode());
                    service.runById(job.getId(), "SCHEDULED");
                } catch (RuntimeException ex) {
                    log.warn("Scheduled integration job {} failed: {}", job.getCode(), ex.getMessage());
                }
            }
        } finally {
            polling.set(false);
        }
    }
}
