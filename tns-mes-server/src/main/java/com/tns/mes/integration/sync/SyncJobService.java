package com.tns.mes.integration.sync;

import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.integration.sap.SapProperties;
import com.tns.mes.integration.sap.SapSyncService;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SyncJobService {
    public static final String PRODUCT_JOB = "SAP_PRODUCT_SYNC";
    public static final String WORK_ORDER_JOB = "SAP_WORK_ORDER_SYNC";
    public static final String BATCH_JOB = "SAP_BATCH_SYNC";

    private final SyncJobRepository jobs;
    private final SyncRunRepository runs;
    private final SapSyncService sapSyncService;
    private final SapProperties sapProperties;

    public SyncJobService(SyncJobRepository jobs, SyncRunRepository runs, SapSyncService sapSyncService,
                          SapProperties sapProperties) {
        this.jobs = jobs;
        this.runs = runs;
        this.sapSyncService = sapSyncService;
        this.sapProperties = sapProperties;
    }

    @Transactional
    public void ensureDefaults() {
        upsertDefault(PRODUCT_JOB, "SAP产品主数据同步", "SAP product master sync", "مزامنة منتجات SAP",
                sapProperties.getProductPath(), "0 0/15 * * * *", "从SAP产品主数据接口同步物料及产品属性", 10);
        upsertDefault(WORK_ORDER_JOB, "SAP生产工单同步", "SAP production order sync", "مزامنة أوامر إنتاج SAP",
                sapProperties.getWorkOrderPath(), "0 5/15 * * * *", "同步生产工单，并获取工单组件和工序", 20);
        upsertDefault(BATCH_JOB, "SAP批次同步", "SAP batch sync", "مزامنة الدفعات SAP",
                sapProperties.getBatchPath(), "0 10/15 * * * *", "从SAP同步批次主数据及状态", 30);
    }

    private void upsertDefault(String code, String zh, String en, String ar, String endpoint,
                               String cron, String description, int sortOrder) {
        SyncJob job = jobs.findByCode(code).orElseGet(SyncJob::new);
        boolean isNew = job.getId() == null;
        job.setCode(code);
        job.setNameZh(zh);
        job.setNameEn(en);
        job.setNameAr(ar);
        job.setSystemCode("SAP");
        job.setEndpoint(endpoint);
        job.setHttpMethod("GET");
        job.setDescription(description);
        job.setSortOrder(sortOrder);
        // A process restart can leave a run marked RUNNING; make it executable again.
        if (!isNew && "RUNNING".equals(job.getStatus())) {
            job.setStatus(Boolean.TRUE.equals(job.getEnabled()) ? "IDLE" : "DISABLED");
            job.setNextRunAt(Boolean.TRUE.equals(job.getEnabled()) ? nextExecution(cron) : null);
        }
        if (isNew) {
            job.setCronExpression(cron);
            job.setEnabled(sapProperties.isScheduleEnabled());
            job.setStatus("IDLE");
            job.setNextRunAt(nextExecution(cron));
        }
        jobs.save(job);
    }

    @Transactional(readOnly = true)
    public List<JobView> list(String keyword, String status) {
        String term = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String wanted = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return jobs.findAll().stream()
                .filter(job -> term.isEmpty() || contains(job.getCode(), term) || contains(job.getNameZh(), term)
                        || contains(job.getNameEn(), term) || contains(job.getEndpoint(), term))
                .filter(job -> wanted.isEmpty() || wanted.equals(job.getStatus())
                        || ("ENABLED".equals(wanted) && Boolean.TRUE.equals(job.getEnabled()))
                        || ("DISABLED".equals(wanted) && !Boolean.TRUE.equals(job.getEnabled())))
                .sorted(Comparator.comparing(SyncJob::getSortOrder).thenComparing(SyncJob::getCode))
                .map(job -> new JobView(job, Collections.emptyList()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobView detail(Long id) {
        SyncJob job = get(id);
        List<RunView> history = runs.findTop20ByJobIdOrderByStartedAtDesc(id).stream()
                .map(RunView::new).collect(Collectors.toList());
        return new JobView(job, history);
    }

    @Transactional
    @Auditable(action = "TOGGLE", resource = "SYNC_JOB")
    public JobView setEnabled(Long id, boolean enabled) {
        SyncJob job = get(id);
        job.setEnabled(enabled);
        job.setStatus(enabled ? "IDLE" : "DISABLED");
        job.setNextRunAt(enabled ? nextExecution(job.getCronExpression()) : null);
        return new JobView(jobs.save(job), Collections.emptyList());
    }

    @Transactional
    @Auditable(action = "UPDATE", resource = "SYNC_JOB")
    public JobView update(Long id, JobUpdateRequest request) {
        SyncJob job = get(id);
        try { CronExpression.parse(request.getCronExpression()); }
        catch (RuntimeException ex) { throw new BizException(4003, "error.validation"); }
        job.setCronExpression(request.getCronExpression().trim());
        job.setDescription(request.getDescription());
        job.setNextRunAt(Boolean.TRUE.equals(job.getEnabled()) ? nextExecution(job.getCronExpression()) : null);
        return new JobView(jobs.save(job), Collections.emptyList());
    }

    public SapSyncService.SyncResult runById(Long id, String triggerType) {
        return execute(get(id), triggerType, null, null);
    }

    @Transactional
    public SapSyncService.SyncResult runByCode(String code, String triggerType, String path, Map<String, ?> query) {
        SyncJob job = jobs.findByCode(code).orElseThrow(() -> new BizException(4041, "error.not-found"));
        return execute(job, triggerType, path, query);
    }

    private SapSyncService.SyncResult execute(SyncJob job, String triggerType, String path, Map<String, ?> query) {
        if ("RUNNING".equals(job.getStatus())) throw new BizException(4092, "error.invalid-state");
        LocalDateTime started = LocalDateTime.now();
        job.setStatus("RUNNING");
        job = jobs.save(job);
        SyncRun run = new SyncRun();
        run.setJob(job);
        run.setTriggerType(triggerType == null ? "MANUAL" : triggerType);
        run.setStatus("RUNNING");
        run.setEndpoint(path == null || path.trim().isEmpty() ? job.getEndpoint() : path.trim());
        run.setHttpMethod(job.getHttpMethod());
        run.setStartedAt(started);
        run.setCreatedAt(started);
        run = runs.save(run);
        try {
            SapSyncService.SyncResult result = PRODUCT_JOB.equals(job.getCode())
                    ? sapSyncService.syncProducts(path, query)
                    : (WORK_ORDER_JOB.equals(job.getCode())
                    ? sapSyncService.syncWorkOrders(path, query)
                    : sapSyncService.syncBatches(path, query));
            run.setReceivedCount(result.getReceived());
            run.setCreatedCount(result.getCreated());
            run.setUpdatedCount(result.getUpdated());
            run.setFailedCount(result.getFailed());
            run.setErrorSummary(joinErrors(result.getErrors()));
            run.setStatus(result.getFailed() > 0 ? "PARTIAL" : "SUCCESS");
            job.setStatus(run.getStatus());
            return result;
        } catch (RuntimeException ex) {
            run.setStatus("FAILED");
            run.setErrorSummary(limit(ex.getMessage(), 2000));
            job.setStatus("FAILED");
            throw ex;
        } finally {
            LocalDateTime finished = LocalDateTime.now();
            run.setFinishedAt(finished);
            job.setLastRunAt(finished);
            job.setNextRunAt(Boolean.TRUE.equals(job.getEnabled()) ? nextExecution(job.getCronExpression()) : null);
            // Persist final state; job is managed in this transaction so save is just a flush.
            jobs.save(job);
            runs.save(run);
        }
    }

    private SyncJob get(Long id) {
        return jobs.findById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
    }

    private LocalDateTime nextExecution(String cron) {
        ZonedDateTime next = CronExpression.parse(cron).next(ZonedDateTime.now());
        return next == null ? null : next.toLocalDateTime();
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private String joinErrors(List<String> errors) {
        if (errors == null || errors.isEmpty()) return null;
        return limit(errors.stream().filter(value -> value != null && !value.trim().isEmpty())
                .collect(Collectors.joining("; ")), 2000);
    }

    private String limit(String value, int length) {
        if (value == null) return null;
        return value.length() <= length ? value : value.substring(0, length);
    }

    public static class JobUpdateRequest {
        @javax.validation.constraints.NotBlank private String cronExpression;
        private String description;
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String value) { cronExpression = value; }
        public String getDescription() { return description; }
        public void setDescription(String value) { description = value; }
    }

    public static class JobView {
        private final Long id;
        private final String code;
        private final String nameZh;
        private final String nameEn;
        private final String nameAr;
        private final String systemCode;
        private final String endpoint;
        private final String httpMethod;
        private final String cronExpression;
        private final String description;
        private final Boolean enabled;
        private final String status;
        private final LocalDateTime lastRunAt;
        private final LocalDateTime nextRunAt;
        private final List<RunView> runs;
        public JobView(SyncJob value, List<RunView> history) {
            id = value.getId(); code = value.getCode(); nameZh = value.getNameZh(); nameEn = value.getNameEn();
            nameAr = value.getNameAr(); systemCode = value.getSystemCode(); endpoint = value.getEndpoint();
            httpMethod = value.getHttpMethod(); cronExpression = value.getCronExpression(); description = value.getDescription();
            enabled = value.getEnabled(); status = value.getStatus(); lastRunAt = value.getLastRunAt();
            nextRunAt = value.getNextRunAt(); runs = history;
        }
        public Long getId(){return id;} public String getCode(){return code;} public String getNameZh(){return nameZh;}
        public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public String getSystemCode(){return systemCode;}
        public String getEndpoint(){return endpoint;} public String getHttpMethod(){return httpMethod;} public String getCronExpression(){return cronExpression;}
        public String getDescription(){return description;} public Boolean getEnabled(){return enabled;} public String getStatus(){return status;}
        public LocalDateTime getLastRunAt(){return lastRunAt;} public LocalDateTime getNextRunAt(){return nextRunAt;} public List<RunView> getRuns(){return runs;}
    }

    public static class RunView {
        private final Long id;
        private final String triggerType;
        private final String status;
        private final String endpoint;
        private final String httpMethod;
        private final LocalDateTime startedAt;
        private final LocalDateTime finishedAt;
        private final Integer received;
        private final Integer created;
        private final Integer updated;
        private final Integer failed;
        private final String errorSummary;
        public RunView(SyncRun value) {
            id=value.getId(); triggerType=value.getTriggerType(); status=value.getStatus(); endpoint=value.getEndpoint();
            httpMethod=value.getHttpMethod(); startedAt=value.getStartedAt(); finishedAt=value.getFinishedAt();
            received=value.getReceivedCount(); created=value.getCreatedCount(); updated=value.getUpdatedCount();
            failed=value.getFailedCount(); errorSummary=value.getErrorSummary();
        }
        public Long getId(){return id;} public String getTriggerType(){return triggerType;} public String getStatus(){return status;}
        public String getEndpoint(){return endpoint;} public String getHttpMethod(){return httpMethod;} public LocalDateTime getStartedAt(){return startedAt;}
        public LocalDateTime getFinishedAt(){return finishedAt;} public Integer getReceived(){return received;} public Integer getCreated(){return created;}
        public Integer getUpdated(){return updated;} public Integer getFailed(){return failed;} public String getErrorSummary(){return errorSummary;}
    }
}
