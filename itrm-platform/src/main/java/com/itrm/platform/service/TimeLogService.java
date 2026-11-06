package com.itrm.platform.service;

import com.itrm.platform.common.BizException;
import com.itrm.platform.domain.Requirement;
import com.itrm.platform.domain.Task;
import com.itrm.platform.domain.TimeLog;
import com.itrm.platform.repo.RequirementRepository;
import com.itrm.platform.repo.TaskRepository;
import com.itrm.platform.repo.TimeLogRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final RequirementRepository requirementRepository;
    private final TaskRepository taskRepository;

    public TimeLogService(TimeLogRepository timeLogRepository,
                          RequirementRepository requirementRepository,
                          TaskRepository taskRepository) {
        this.timeLogRepository = timeLogRepository;
        this.requirementRepository = requirementRepository;
        this.taskRepository = taskRepository;
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional(readOnly = true)
    public List<TimeLog> listByRequirement(Long requirementId) {
        return timeLogRepository.findByRequirementIdOrderByWorkDateDesc(requirementId);
    }

    @Transactional(readOnly = true)
    public List<TimeLog> listByWorker(String worker) {
        return timeLogRepository.findByWorkerOrderByWorkDateDesc(worker);
    }

    @Transactional
    public TimeLog create(Long taskId, TimeLog log) {
        if (taskId == null) {
            throw new BizException(400, "工时必须关联开发任务");
        }
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(404, "任务不存在"));
        Long requirementId = task.getRequirementId();
        Requirement req = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new BizException(404, "任务所属需求不存在"));
        if (log.getHours() == null || log.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "工时必须大于 0");
        }

        TimeLog newLog = new TimeLog();
        newLog.setRequirementId(requirementId);
        newLog.setTaskId(taskId);
        newLog.setWorker(log.getWorker() != null ? log.getWorker() : getCurrentUser());
        newLog.setWorkDate(log.getWorkDate() != null ? log.getWorkDate() : LocalDate.now());
        newLog.setHours(log.getHours());
        newLog.setWorkType(log.getWorkType() != null ? log.getWorkType().toUpperCase() : "DEV");
        newLog.setDescription(log.getDescription());
        newLog.setCreatedAt(Instant.now());
        newLog.setCreatedBy(getCurrentUser());

        TimeLog saved = timeLogRepository.save(newLog);

        // 同步更新需求的 actualHours
        req.setActualHours(safeHours(req.getActualHours()).add(log.getHours()));
        requirementRepository.save(req);

        // 同步更新任务的 actualHours
        task.setActualHours(safeHours(task.getActualHours()).add(log.getHours()));
        taskRepository.save(task);

        return saved;
    }

    @Transactional
    public void delete(Long id) {
        TimeLog log = timeLogRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "工时记录不存在"));

        // 同步扣减需求的实际工时
        Requirement req = requirementRepository.findById(log.getRequirementId()).orElse(null);
        if (req != null) {
            req.setActualHours(safeHours(req.getActualHours()).subtract(log.getHours()).max(BigDecimal.ZERO));
            requirementRepository.save(req);
        }

        // 同步扣减任务的实际工时
        if (log.getTaskId() != null) {
            Task task = taskRepository.findById(log.getTaskId()).orElse(null);
            if (task != null) {
                task.setActualHours(safeHours(task.getActualHours()).subtract(log.getHours()).max(BigDecimal.ZERO));
                taskRepository.save(task);
            }
        }

        timeLogRepository.delete(log);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> statsByWorker(String worker, LocalDate startDate, LocalDate endDate) {
        Specification<TimeLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("worker"), worker));
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("workDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("workDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<TimeLog> logs = timeLogRepository.findAll(spec);

        BigDecimal totalHours = BigDecimal.ZERO;
        Map<String, BigDecimal> byWorkType = new HashMap<>();
        Map<LocalDate, BigDecimal> byDate = new HashMap<>();

        for (TimeLog log : logs) {
            totalHours = totalHours.add(log.getHours());

            String workType = log.getWorkType() != null ? log.getWorkType() : "DEV";
            byWorkType.put(workType, byWorkType.getOrDefault(workType, BigDecimal.ZERO).add(log.getHours()));

            LocalDate date = log.getWorkDate();
            byDate.put(date, byDate.getOrDefault(date, BigDecimal.ZERO).add(log.getHours()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("worker", worker);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalHours", totalHours);
        result.put("byWorkType", byWorkType);
        result.put("byDate", byDate);
        result.put("recordCount", logs.size());
        return result;
    }

    private BigDecimal safeHours(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
