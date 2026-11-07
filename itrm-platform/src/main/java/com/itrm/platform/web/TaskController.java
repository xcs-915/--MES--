package com.itrm.platform.web;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.domain.Task;
import com.itrm.platform.service.TaskService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/by-requirement/{reqId}")
    public ApiResponse<List<Task>> listByRequirement(@PathVariable Long reqId) {
        return ApiResponse.ok(taskService.listByRequirement(reqId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Task> get(@PathVariable Long id) {
        return ApiResponse.ok(taskService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('task:create')")
    public ApiResponse<Task> create(@RequestBody Map<String, Object> body) {
        Long requirementId = Long.valueOf(body.get("requirementId").toString());
        Task task = new Task();
        task.setTitle((String) body.get("title"));
        task.setDescription((String) body.get("description"));
        task.setPriority((String) body.get("priority"));
        task.setAssignee((String) body.get("assignee"));
        if (body.get("estimatedHours") != null) {
            task.setEstimatedHours(new java.math.BigDecimal(body.get("estimatedHours").toString()));
        }
        if (body.get("startDate") != null) {
            task.setStartDate(java.time.LocalDate.parse(body.get("startDate").toString()));
        }
        if (body.get("dueDate") != null) {
            task.setDueDate(java.time.LocalDate.parse(body.get("dueDate").toString()));
        }
        task.setRemark((String) body.get("remark"));
        return ApiResponse.ok(taskService.create(requirementId, task));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('task:edit')")
    public ApiResponse<Task> update(@PathVariable Long id, @RequestBody Task task) {
        return ApiResponse.ok(taskService.update(id, task));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('task:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('task:transition')")
    public ApiResponse<Task> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ApiResponse.ok(taskService.changeStatus(id, status));
    }

    @PutMapping("/{id}/assignee")
    @PreAuthorize("hasAuthority('task:assign')")
    public ApiResponse<Task> updateAssignee(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String assignee = body.get("assignee");
        return ApiResponse.ok(taskService.updateAssignee(id, assignee));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('task:accept')")
    public ApiResponse<Task> accept(@PathVariable Long id) {
        return ApiResponse.ok(taskService.accept(id));
    }

    @PutMapping("/{id}/progress")
    @PreAuthorize("hasAuthority('task:transition')")
    public ApiResponse<Task> updateProgress(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer progress = body.get("progress");
        return ApiResponse.ok(taskService.updateProgress(id, progress));
    }

    @PutMapping("/{id}/move")
    @PreAuthorize("hasAuthority('board:move')")
    public ApiResponse<Task> moveTask(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String direction = body.get("direction");
        return ApiResponse.ok(taskService.moveTask(id, direction));
    }
}
