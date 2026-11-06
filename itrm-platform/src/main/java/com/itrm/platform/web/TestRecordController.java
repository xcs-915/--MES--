package com.itrm.platform.web;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.domain.TestRecord;
import com.itrm.platform.service.TestRecordService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-records")
public class TestRecordController {

    private final TestRecordService testRecordService;

    public TestRecordController(TestRecordService testRecordService) {
        this.testRecordService = testRecordService;
    }

    @GetMapping("/by-requirement/{reqId}")
    public ApiResponse<List<TestRecord>> listByRequirement(@PathVariable Long reqId) {
        return ApiResponse.ok(testRecordService.listByRequirement(reqId));
    }

    @GetMapping("/{id}")
    public ApiResponse<TestRecord> get(@PathVariable Long id) {
        return ApiResponse.ok(testRecordService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('test:create')")
    public ApiResponse<TestRecord> create(@RequestBody Map<String, Object> body) {
        Long requirementId = Long.valueOf(body.get("requirementId").toString());
        Long taskId = body.get("taskId") != null ? Long.valueOf(body.get("taskId").toString()) : null;
        TestRecord record = new TestRecord();
        record.setTitle((String) body.get("title"));
        record.setType((String) body.get("type"));
        record.setSeverity((String) body.get("severity"));
        record.setTester((String) body.get("tester"));
        record.setHandler((String) body.get("handler"));
        record.setDescription((String) body.get("description"));
        record.setExpectedResult((String) body.get("expectedResult"));
        record.setActualResult((String) body.get("actualResult"));
        record.setAttachments((String) body.get("attachments"));
        return ApiResponse.ok(testRecordService.create(requirementId, taskId, record));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('test:edit')")
    public ApiResponse<TestRecord> update(@PathVariable Long id, @RequestBody TestRecord record) {
        return ApiResponse.ok(testRecordService.update(id, record));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('test:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        testRecordService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('test:transition')")
    public ApiResponse<TestRecord> resolve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String handler = body != null ? body.get("handler") : null;
        return ApiResponse.ok(testRecordService.resolve(id, handler));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('test:transition')")
    public ApiResponse<TestRecord> close(@PathVariable Long id) {
        return ApiResponse.ok(testRecordService.close(id));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('test:transition')")
    public ApiResponse<TestRecord> accept(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String result = body.get("result");
        String acceptBy = body.get("acceptBy");
        return ApiResponse.ok(testRecordService.accept(id, result, acceptBy));
    }
}
