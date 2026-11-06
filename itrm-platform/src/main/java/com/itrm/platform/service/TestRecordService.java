package com.itrm.platform.service;

import com.itrm.platform.common.BizException;
import com.itrm.platform.domain.TestRecord;
import com.itrm.platform.domain.Task;
import com.itrm.platform.repo.RequirementRepository;
import com.itrm.platform.repo.TaskRepository;
import com.itrm.platform.repo.TestRecordRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TestRecordService {

    private final TestRecordRepository testRecordRepository;
    private final RequirementRepository requirementRepository;
    private final TaskRepository taskRepository;

    public TestRecordService(TestRecordRepository testRecordRepository,
                             RequirementRepository requirementRepository,
                             TaskRepository taskRepository) {
        this.testRecordRepository = testRecordRepository;
        this.requirementRepository = requirementRepository;
        this.taskRepository = taskRepository;
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional(readOnly = true)
    public List<TestRecord> listByRequirement(Long requirementId) {
        return testRecordRepository.findByRequirementIdOrderByCreatedAtDesc(requirementId);
    }

    @Transactional(readOnly = true)
    public TestRecord get(Long id) {
        return testRecordRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "测试记录不存在"));
    }

    @Transactional
    public TestRecord create(Long requirementId, Long taskId, TestRecord record) {
        requirementRepository.findById(requirementId)
                .orElseThrow(() -> new BizException(404, "需求不存在"));
        if (taskId != null) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new BizException(404, "开发任务不存在"));
            if (!requirementId.equals(task.getRequirementId())) {
                throw new BizException(400, "测试记录与开发任务不属于同一需求");
            }
        }
        if (record.getTitle() == null || record.getTitle().trim().isEmpty()) {
            throw new BizException(400, "测试/缺陷标题不能为空");
        }
        TestRecord newRecord = new TestRecord();
        newRecord.setRequirementId(requirementId);
        newRecord.setTaskId(taskId);
        newRecord.setTitle(record.getTitle());
        newRecord.setType(record.getType() != null ? record.getType() : "TEST");
        newRecord.setSeverity(record.getSeverity() != null ? record.getSeverity() : "LOW");
        newRecord.setStatus("OPEN");
        newRecord.setTester(record.getTester() != null ? record.getTester() : getCurrentUser());
        newRecord.setHandler(record.getHandler());
        newRecord.setDescription(record.getDescription());
        newRecord.setExpectedResult(record.getExpectedResult());
        newRecord.setActualResult(record.getActualResult());
        newRecord.setAttachments(record.getAttachments());
        return testRecordRepository.save(newRecord);
    }

    @Transactional
    public TestRecord update(Long id, TestRecord record) {
        TestRecord existing = get(id);
        existing.setTitle(record.getTitle());
        existing.setType(record.getType() != null ? record.getType() : existing.getType());
        existing.setSeverity(record.getSeverity() != null ? record.getSeverity() : existing.getSeverity());
        existing.setHandler(record.getHandler());
        existing.setDescription(record.getDescription());
        existing.setExpectedResult(record.getExpectedResult());
        existing.setActualResult(record.getActualResult());
        existing.setAttachments(record.getAttachments());
        return testRecordRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        TestRecord record = get(id);
        testRecordRepository.delete(record);
    }

    @Transactional
    public TestRecord resolve(Long id, String handler) {
        TestRecord record = get(id);
        if (!"OPEN".equals(record.getStatus())) {
            throw new BizException(400, "只有打开状态的记录可以解决");
        }
        record.setStatus("RESOLVED");
        record.setHandler(handler != null ? handler : getCurrentUser());
        return testRecordRepository.save(record);
    }

    @Transactional
    public TestRecord close(Long id) {
        TestRecord record = get(id);
        if (!"RESOLVED".equals(record.getStatus())) {
            throw new BizException(400, "只有已解决状态的记录可以关闭");
        }
        record.setStatus("CLOSED");
        return testRecordRepository.save(record);
    }

    @Transactional
    public TestRecord accept(Long id, String result, String acceptBy) {
        TestRecord record = get(id);
        if (!"RESOLVED".equals(record.getStatus())) {
            throw new BizException(400, "只有已解决状态的记录可以验收");
        }
        record.setAcceptResult(result);
        record.setAcceptBy(acceptBy != null ? acceptBy : getCurrentUser());
        record.setAcceptAt(Instant.now());
        if ("PASS".equals(result)) {
            record.setStatus("CLOSED");
        } else if ("FAIL".equals(result)) {
            record.setStatus("OPEN");
        }
        return testRecordRepository.save(record);
    }
}
