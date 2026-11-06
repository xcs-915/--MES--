package com.itrm.platform.service;

import com.itrm.platform.common.BizException;
import com.itrm.platform.domain.Requirement;
import com.itrm.platform.domain.Task;
import com.itrm.platform.repo.RequirementRepository;
import com.itrm.platform.repo.TaskRepository;
import com.itrm.platform.repo.TimeLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final RequirementRepository requirementRepository;
    private final TimeLogRepository timeLogRepository;
    private static final Set<String> TASK_READY_STATUSES = new HashSet<>(Arrays.asList(
            "PENDING_PLAN", "IN_DEVELOPMENT", "IN_TESTING", "PENDING_RELEASE"
    ));

    public TaskService(TaskRepository taskRepository, RequirementRepository requirementRepository,
                       TimeLogRepository timeLogRepository) {
        this.taskRepository = taskRepository;
        this.requirementRepository = requirementRepository;
        this.timeLogRepository = timeLogRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> listByRequirement(Long requirementId) {
        return taskRepository.findByRequirementIdOrderBySortOrderAsc(requirementId);
    }

    @Transactional(readOnly = true)
    public Task get(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "任务不存在"));
    }

    @Transactional
    public Task create(Long requirementId, Task task) {
        Requirement req = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new BizException(404, "需求不存在"));
        if (!TASK_READY_STATUSES.contains(req.getStatus())) {
            throw new BizException(400, "需求接收并进入排期后才能拆分开发任务");
        }
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new BizException(400, "任务标题不能为空");
        }

        Task newTask = new Task();
        newTask.setRequirementId(requirementId);
        newTask.setTaskNo(generateTaskNo(requirementId));
        newTask.setTitle(task.getTitle());
        newTask.setDescription(task.getDescription());
        newTask.setStatus("TODO");
        newTask.setPriority(task.getPriority() != null ? task.getPriority() : "MEDIUM");
        newTask.setAssignee(task.getAssignee());
        newTask.setEstimatedHours(task.getEstimatedHours() != null ? task.getEstimatedHours() : BigDecimal.ZERO);
        newTask.setStartDate(task.getStartDate());
        newTask.setDueDate(task.getDueDate());
        newTask.setSortOrder(getNextSortOrder(requirementId));
        newTask.setRemark(task.getRemark());

        Task saved = taskRepository.save(newTask);

        // 更新需求的预计工时总和
        updateRequirementEstimatedHours(requirementId);

        return saved;
    }

    private String generateTaskNo(Long requirementId) {
        List<Task> tasks = taskRepository.findByRequirementIdOrderBySortOrderAsc(requirementId);
        int seq = tasks.size() + 1;
        // 确保3位序号
        return "T" + requirementId + String.format("%03d", seq);
    }

    private int getNextSortOrder(Long requirementId) {
        List<Task> tasks = taskRepository.findByRequirementIdOrderBySortOrderAsc(requirementId);
        if (tasks.isEmpty()) {
            return 1;
        }
        return tasks.get(tasks.size() - 1).getSortOrder() + 1;
    }

    private void updateRequirementEstimatedHours(Long requirementId) {
        List<Task> tasks = taskRepository.findByRequirementIdOrderBySortOrderAsc(requirementId);
        BigDecimal total = BigDecimal.ZERO;
        for (Task task : tasks) {
            if (task.getEstimatedHours() != null) {
                total = total.add(task.getEstimatedHours());
            }
        }
        Requirement req = requirementRepository.findById(requirementId).orElse(null);
        if (req != null) {
            req.setEstimatedHours(total);
            requirementRepository.save(req);
        }
    }

    private void updateRequirementProgress(Long requirementId) {
        List<Task> tasks = taskRepository.findByRequirementIdOrderBySortOrderAsc(requirementId);
        if (tasks.isEmpty()) {
            return;
        }
        int totalProgress = 0;
        for (Task task : tasks) {
            totalProgress += task.getProgress() != null ? task.getProgress() : 0;
        }
        int avgProgress = totalProgress / tasks.size();
        Requirement req = requirementRepository.findById(requirementId).orElse(null);
        if (req != null) {
            req.setProgress(avgProgress);
            requirementRepository.save(req);
        }
    }

    @Transactional
    public Task update(Long id, Task task) {
        Task existing = get(id);
        existing.setTitle(task.getTitle());
        existing.setDescription(task.getDescription());
        existing.setPriority(task.getPriority() != null ? task.getPriority() : existing.getPriority());
        existing.setAssignee(task.getAssignee());
        existing.setEstimatedHours(task.getEstimatedHours() != null ? task.getEstimatedHours() : existing.getEstimatedHours());
        existing.setStartDate(task.getStartDate());
        existing.setDueDate(task.getDueDate());
        existing.setRemark(task.getRemark());

        Task saved = taskRepository.save(existing);

        // 更新需求的预计工时总和
        updateRequirementEstimatedHours(existing.getRequirementId());

        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Task task = get(id);
        if (timeLogRepository.existsByTaskId(id)) {
            throw new BizException(400, "该任务已有工时记录，不能删除");
        }
        Long requirementId = task.getRequirementId();
        taskRepository.delete(task);
        // 更新需求的预计工时总和
        updateRequirementEstimatedHours(requirementId);
    }

    @Transactional
    public Task changeStatus(Long id, String status) {
        Task task = get(id);
        task.setStatus(status);
        if ("DONE".equals(status) || "COMPLETED".equals(status)) {
            task.setCompletedDate(LocalDate.now());
            task.setProgress(100);
        } else {
            task.setCompletedDate(null);
        }
        Task saved = taskRepository.save(task);
        // 更新需求进度
        updateRequirementProgress(task.getRequirementId());
        return saved;
    }

    @Transactional
    public Task updateAssignee(Long id, String assignee) {
        Task task = get(id);
        task.setAssignee(assignee);
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateProgress(Long id, Integer progress) {
        if (progress == null || progress < 0 || progress > 100) {
            throw new BizException(400, "进度值必须在0-100之间");
        }
        Task task = get(id);
        task.setProgress(progress);
        Task saved = taskRepository.save(task);
        // 更新需求的整体进度
        updateRequirementProgress(task.getRequirementId());
        return saved;
    }

    @Transactional
    public Task moveTask(Long id, String direction) {
        Task task = get(id);
        Long requirementId = task.getRequirementId();
        List<Task> tasks = taskRepository.findByRequirementIdOrderBySortOrderAsc(requirementId);

        int currentIndex = -1;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(id)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            throw new BizException(400, "任务不在当前需求的任务列表中");
        }

        if ("up".equals(direction)) {
            if (currentIndex == 0) {
                throw new BizException(400, "已经是第一个任务，无法上移");
            }
            Task prev = tasks.get(currentIndex - 1);
            int temp = prev.getSortOrder();
            prev.setSortOrder(task.getSortOrder());
            task.setSortOrder(temp);
            taskRepository.save(prev);
        } else if ("down".equals(direction)) {
            if (currentIndex == tasks.size() - 1) {
                throw new BizException(400, "已经是最后一个任务，无法下移");
            }
            Task next = tasks.get(currentIndex + 1);
            int temp = next.getSortOrder();
            next.setSortOrder(task.getSortOrder());
            task.setSortOrder(temp);
            taskRepository.save(next);
        } else {
            throw new BizException(400, "无效的移动方向: " + direction);
        }

        return taskRepository.save(task);
    }
}
