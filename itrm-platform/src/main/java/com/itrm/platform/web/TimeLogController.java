package com.itrm.platform.web;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.domain.TimeLog;
import com.itrm.platform.service.TimeLogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/time-logs")
public class TimeLogController {

    private final TimeLogService timeLogService;

    public TimeLogController(TimeLogService timeLogService) {
        this.timeLogService = timeLogService;
    }

    @GetMapping("/by-requirement/{reqId}")
    public ApiResponse<List<TimeLog>> listByRequirement(@PathVariable Long reqId) {
        return ApiResponse.ok(timeLogService.listByRequirement(reqId));
    }

    @GetMapping("/by-worker/{worker}")
    public ApiResponse<List<TimeLog>> listByWorker(@PathVariable String worker) {
        return ApiResponse.ok(timeLogService.listByWorker(worker));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('timelog:create')")
    public ApiResponse<TimeLog> create(@RequestBody Map<String, Object> body) {
        Long taskId = body.get("taskId") != null && !body.get("taskId").toString().trim().isEmpty()
                ? Long.valueOf(body.get("taskId").toString()) : null;
        TimeLog log = new TimeLog();
        log.setWorker((String) body.get("worker"));
        if (body.get("workDate") != null) {
            log.setWorkDate(LocalDate.parse(body.get("workDate").toString()));
        }
        if (body.get("hours") != null) {
            log.setHours(new BigDecimal(body.get("hours").toString()));
        }
        log.setWorkType((String) body.get("workType"));
        log.setDescription((String) body.get("description"));
        return ApiResponse.ok(timeLogService.create(taskId, log));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('timelog:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        timeLogService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/stats/worker/{worker}")
    public ApiResponse<Map<String, Object>> statsByWorker(
            @PathVariable String worker,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        return ApiResponse.ok(timeLogService.statsByWorker(worker, start, end));
    }
}
