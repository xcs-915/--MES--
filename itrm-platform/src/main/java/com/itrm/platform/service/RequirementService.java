package com.itrm.platform.service;

import com.itrm.platform.common.BizException;
import com.itrm.platform.common.PageResponse;
import com.itrm.platform.domain.*;
import com.itrm.platform.repo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class RequirementService {

    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList(
            "NEW_FUNC", "FUNC_OPT", "BUG", "INTERFACE", "REPORT", "OTHER"
    ));
    private static final Set<String> SYSTEMS = new HashSet<>(Arrays.asList(
            "MES", "WMS", "SAP", "OA", "SCADA", "OTHER"
    ));

    private final RequirementRepository requirementRepository;
    private final TaskRepository taskRepository;
    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalNodeRepository approvalNodeRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final ChangeLogRepository changeLogRepository;

    public RequirementService(RequirementRepository requirementRepository,
                              TaskRepository taskRepository,
                              ApprovalTemplateRepository approvalTemplateRepository,
                              ApprovalNodeRepository approvalNodeRepository,
                              ApprovalRecordRepository approvalRecordRepository,
                              ChangeLogRepository changeLogRepository) {
        this.requirementRepository = requirementRepository;
        this.taskRepository = taskRepository;
        this.approvalTemplateRepository = approvalTemplateRepository;
        this.approvalNodeRepository = approvalNodeRepository;
        this.approvalRecordRepository = approvalRecordRepository;
        this.changeLogRepository = changeLogRepository;
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // ==================== 查询 ====================

    @Transactional(readOnly = true)
    public PageResponse<Requirement> page(String keyword, String status, String urgency,
                                          String category, String sourceDept, String assignee,
                                          int page, int size) {
        Specification<Requirement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("reqNo"), like),
                        cb.like(root.get("title"), like)
                ));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(urgency)) {
                predicates.add(cb.equal(root.get("urgency"), urgency));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (StringUtils.hasText(sourceDept)) {
                predicates.add(cb.equal(root.get("sourceDept"), sourceDept));
            }
            if (StringUtils.hasText(assignee)) {
                predicates.add(cb.equal(root.get("devOwner"), assignee));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 500));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Requirement> result = requirementRepository.findAll(spec, pageable);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public Requirement get(Long id) {
        return requirementRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "需求不存在"));
    }

    // ==================== CRUD ====================

    @Transactional
    public Requirement create(Requirement request) {
        validateInput(request);
        Requirement req = new Requirement();
        req.setReqNo(generateReqNo());
        req.setTitle(request.getTitle());
        req.setCategory(request.getCategory());
        req.setUrgency(request.getUrgency() != null ? request.getUrgency() : "MEDIUM");
        req.setBelongSystem(request.getBelongSystem());
        req.setSourceDept(request.getSourceDept());
        req.setProposer(request.getProposer());
        req.setDescription(request.getDescription());
        req.setCurrentProblem(request.getCurrentProblem());
        req.setExpectedEffect(request.getExpectedEffect());
        req.setAcceptanceCriteria(request.getAcceptanceCriteria());
        req.setStatus("DRAFT");
        req.setFeasibility(request.getFeasibility());
        req.setWorkloadEstimate(request.getWorkloadEstimate());
        req.setAttachments(request.getAttachments());
        req.setTags(request.getTags());
        req.setRemark(request.getRemark());
        return requirementRepository.save(req);
    }

    private String generateReqNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "REQ" + dateStr;
        long count = requirementRepository.findAll((Specification<Requirement>) (root, query, cb) ->
                cb.like(root.get("reqNo"), prefix + "%")
        ).size();
        int seq = (int) count + 1;
        String reqNo = prefix + String.format("%04d", seq);
        while (requirementRepository.existsByReqNo(reqNo)) {
            seq++;
            reqNo = prefix + String.format("%04d", seq);
        }
        return reqNo;
    }

    @Transactional
    public Requirement update(Long id, Requirement request) {
        Requirement req = get(id);
        if (!"DRAFT".equals(req.getStatus())) {
            throw new BizException(400, "只有草稿状态的需求可以修改");
        }
        validateInput(request);
        req.setTitle(request.getTitle());
        req.setCategory(request.getCategory());
        req.setUrgency(request.getUrgency());
        req.setBelongSystem(request.getBelongSystem());
        req.setSourceDept(request.getSourceDept());
        req.setProposer(request.getProposer());
        req.setDescription(request.getDescription());
        req.setCurrentProblem(request.getCurrentProblem());
        req.setExpectedEffect(request.getExpectedEffect());
        req.setAcceptanceCriteria(request.getAcceptanceCriteria());
        req.setFeasibility(request.getFeasibility());
        req.setWorkloadEstimate(request.getWorkloadEstimate());
        req.setAttachments(request.getAttachments());
        req.setTags(request.getTags());
        req.setRemark(request.getRemark());
        return requirementRepository.save(req);
    }

    private void validateInput(Requirement request) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw new BizException(400, "需求标题不能为空");
        }
        if (!CATEGORIES.contains(request.getCategory())) {
            throw new BizException(400, "需求类型无效");
        }
        if (!SYSTEMS.contains(request.getBelongSystem())) {
            throw new BizException(400, "所属系统无效");
        }
    }

    @Transactional
    public void delete(Long id) {
        Requirement req = get(id);
        if (!"DRAFT".equals(req.getStatus())) {
            throw new BizException(400, "只有草稿状态的需求可以删除");
        }
        requirementRepository.delete(req);
    }

    // ==================== 状态机流转 ====================

    /**
     * 提交需求：DRAFT → PENDING_RECEIVE
     */
    @Transactional
    public Requirement submit(Long id) {
        Requirement req = get(id);
        if (!"DRAFT".equals(req.getStatus())) {
            throw new BizException(400, "只有草稿状态的需求可以提交");
        }
        String fromStatus = req.getStatus();
        req.setStatus("PENDING_RECEIVE");
        req.setSubmittedAt(Instant.now());
        requirementRepository.save(req);
        saveChangeLog(id, "status", fromStatus, "PENDING_RECEIVE", "提交需求");
        return req;
    }

    /**
     * IT部门接收：PENDING_RECEIVE → PENDING_PLAN
     * 设置评审负责人、接收时间，选择审批模板，初始化审批记录
     */
    @Transactional
    public Requirement receive(Long id, String reviewOwner, Long templateId) {
        Requirement req = get(id);
        if (!"PENDING_RECEIVE".equals(req.getStatus())) {
            throw new BizException(400, "只有待接收状态的需求可以接收");
        }
        if (!StringUtils.hasText(reviewOwner)) {
            throw new BizException(400, "评审负责人不能为空");
        }
        if (templateId == null) {
            throw new BizException(400, "审批模板不能为空");
        }

        ApprovalTemplate template = approvalTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BizException(404, "审批模板不存在"));
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BizException(400, "审批模板未启用");
        }

        String fromStatus = req.getStatus();
        req.setStatus("PENDING_PLAN");
        req.setReviewOwner(reviewOwner);
        req.setReceivedAt(Instant.now());
        req.setApprovalTemplateId(templateId);
        req.setApprovalNodeIdx(0);
        requirementRepository.save(req);

        // 初始化审批记录：创建第一个节点的待审批记录
        List<ApprovalNode> nodes = approvalNodeRepository.findByTemplateIdOrderBySortOrderAsc(templateId);
        if (!nodes.isEmpty()) {
            ApprovalNode firstNode = nodes.get(0);
            ApprovalRecord record = new ApprovalRecord();
            record.setRequirementId(id);
            record.setTemplateId(templateId);
            record.setNodeIdx(0);
            record.setNodeName(firstNode.getNodeName());
            record.setNodeType(firstNode.getNodeType());
            record.setAction("PENDING");
            record.setCreatedAt(Instant.now());
            approvalRecordRepository.save(record);
        }

        saveChangeLog(id, "status", fromStatus, "PENDING_PLAN", "IT接收需求");
        return req;
    }

    /**
     * 审批当前节点
     * - 通过：进入下一节点；如果是最后一个审批节点，进入开发阶段 IN_DEVELOPMENT
     * - 驳回：状态变 REJECTED
     */
    @Transactional
    public Requirement approveNode(Long id, String action, String comment) {
        Requirement req = get(id);
        if (!"PENDING_PLAN".equals(req.getStatus())) {
            throw new BizException(400, "只有待排期状态的需求可以审批");
        }
        if (req.getApprovalTemplateId() == null) {
            throw new BizException(400, "未设置审批模板");
        }

        List<ApprovalNode> nodes = approvalNodeRepository.findByTemplateIdOrderBySortOrderAsc(req.getApprovalTemplateId());
        if (nodes.isEmpty()) {
            throw new BizException(400, "审批模板无节点配置");
        }

        int currentIdx = req.getApprovalNodeIdx() != null ? req.getApprovalNodeIdx() : 0;
        if (currentIdx >= nodes.size()) {
            throw new BizException(400, "当前已完成所有审批节点");
        }

        ApprovalNode currentNode = nodes.get(currentIdx);
        String approver = getCurrentUser();
        String fromStatus = req.getStatus();

        if ("APPROVE".equals(action)) {
            // 更新当前节点的审批记录为已通过
            List<ApprovalRecord> records = approvalRecordRepository.findByRequirementIdOrderByCreatedAtDesc(id);
            ApprovalRecord currentRecord = null;
            for (ApprovalRecord r : records) {
                if (r.getNodeIdx() != null && r.getNodeIdx() == currentIdx && "PENDING".equals(r.getAction())) {
                    currentRecord = r;
                    break;
                }
            }
            if (currentRecord != null) {
                currentRecord.setAction("APPROVE");
                currentRecord.setApprover(approver);
                currentRecord.setComment(comment);
                currentRecord.setActedAt(Instant.now());
                approvalRecordRepository.save(currentRecord);
            }

            int nextIdx = currentIdx + 1;
            if (nextIdx >= nodes.size()) {
                // 最后一个审批节点，进入开发阶段
                req.setStatus("IN_DEVELOPMENT");
                req.setApprovalNodeIdx(nextIdx);
                req.setActualStartDate(LocalDate.now());
                requirementRepository.save(req);
                saveChangeLog(id, "status", fromStatus, "IN_DEVELOPMENT", "审批通过，进入开发阶段");
            } else {
                // 进入下一节点
                req.setApprovalNodeIdx(nextIdx);
                requirementRepository.save(req);

                // 创建下一节点的待审批记录
                ApprovalNode nextNode = nodes.get(nextIdx);
                ApprovalRecord nextRecord = new ApprovalRecord();
                nextRecord.setRequirementId(id);
                nextRecord.setTemplateId(req.getApprovalTemplateId());
                nextRecord.setNodeIdx(nextIdx);
                nextRecord.setNodeName(nextNode.getNodeName());
                nextRecord.setNodeType(nextNode.getNodeType());
                nextRecord.setAction("PENDING");
                nextRecord.setCreatedAt(Instant.now());
                approvalRecordRepository.save(nextRecord);

                saveChangeLog(id, "approvalNode", String.valueOf(currentIdx), String.valueOf(nextIdx),
                        "审批节点通过：" + currentNode.getNodeName());
            }
        } else if ("REJECT".equals(action)) {
            // 驳回
            List<ApprovalRecord> records = approvalRecordRepository.findByRequirementIdOrderByCreatedAtDesc(id);
            ApprovalRecord currentRecord = null;
            for (ApprovalRecord r : records) {
                if (r.getNodeIdx() != null && r.getNodeIdx() == currentIdx && "PENDING".equals(r.getAction())) {
                    currentRecord = r;
                    break;
                }
            }
            if (currentRecord != null) {
                currentRecord.setAction("REJECT");
                currentRecord.setApprover(approver);
                currentRecord.setComment(comment);
                currentRecord.setActedAt(Instant.now());
                approvalRecordRepository.save(currentRecord);
            }

            req.setStatus("REJECTED");
            req.setReviewComment(comment);
            requirementRepository.save(req);
            saveChangeLog(id, "status", fromStatus, "REJECTED", "审批驳回：" + currentNode.getNodeName());
        } else {
            throw new BizException(400, "无效的审批操作: " + action);
        }

        return req;
    }

    /**
     * 排期分配开发：PENDING_PLAN → IN_DEVELOPMENT
     */
    @Transactional
    public Requirement plan(Long id, String devOwner, LocalDate plannedStart, LocalDate plannedEnd,
                            LocalDate devPlannedEnd, BigDecimal estimatedHours) {
        Requirement req = get(id);
        if (!"PENDING_PLAN".equals(req.getStatus())) {
            throw new BizException(400, "只有待排期状态的需求可以排期");
        }
        if (!StringUtils.hasText(devOwner)) {
            throw new BizException(400, "开发负责人不能为空");
        }
        String fromStatus = req.getStatus();
        req.setStatus("IN_DEVELOPMENT");
        req.setDevOwner(devOwner);
        req.setPlannedStartDate(plannedStart);
        req.setPlannedEndDate(plannedEnd);
        req.setDevPlannedEnd(devPlannedEnd);
        req.setEstimatedHours(estimatedHours != null ? estimatedHours : BigDecimal.ZERO);
        req.setActualStartDate(LocalDate.now());
        requirementRepository.save(req);
        saveChangeLog(id, "status", fromStatus, "IN_DEVELOPMENT", "排期分配开发");
        return req;
    }

    /**
     * 转测试：IN_DEVELOPMENT → IN_TESTING
     */
    @Transactional
    public Requirement startTesting(Long id, String tester) {
        Requirement req = get(id);
        if (!"IN_DEVELOPMENT".equals(req.getStatus())) {
            throw new BizException(400, "只有开发中的需求可以转测试");
        }
        if (!StringUtils.hasText(tester)) {
            throw new BizException(400, "测试人员不能为空");
        }
        String fromStatus = req.getStatus();
        req.setStatus("IN_TESTING");
        req.setTester(tester);
        req.setTestStartDate(LocalDate.now());
        requirementRepository.save(req);
        saveChangeLog(id, "status", fromStatus, "IN_TESTING", "转测试");
        return req;
    }

    /**
     * 测试完成：IN_TESTING → PENDING_RELEASE
     */
    @Transactional
    public Requirement finishTesting(Long id) {
        Requirement req = get(id);
        if (!"IN_TESTING".equals(req.getStatus())) {
            throw new BizException(400, "只有测试中的需求可以完成测试");
        }
        String fromStatus = req.getStatus();
        req.setStatus("PENDING_RELEASE");
        req.setTestEndDate(LocalDate.now());
        requirementRepository.save(req);
        saveChangeLog(id, "status", fromStatus, "PENDING_RELEASE", "测试通过，待发布");
        return req;
    }

    /**
     * 发布
     * success=true: PENDING_RELEASE → COMPLETED
     * success=false: PENDING_RELEASE → IN_DEVELOPMENT（回退改BUG）
     */
    @Transactional
    public Requirement release(Long id, boolean success) {
        Requirement req = get(id);
        if (!"PENDING_RELEASE".equals(req.getStatus())) {
            throw new BizException(400, "只有待发布状态的需求可以发布");
        }
        String fromStatus = req.getStatus();
        if (success) {
            req.setStatus("COMPLETED");
            req.setReleaseDate(LocalDate.now());
            req.setCompleteDate(LocalDate.now());
            requirementRepository.save(req);
            saveChangeLog(id, "status", fromStatus, "COMPLETED", "发布成功");
        } else {
            req.setStatus("IN_DEVELOPMENT");
            requirementRepository.save(req);
            saveChangeLog(id, "status", fromStatus, "IN_DEVELOPMENT", "发布失败，回退开发改BUG");
        }
        return req;
    }

    /**
     * 驳回需求（从 PENDING_RECEIVE 状态）
     */
    @Transactional
    public Requirement reject(Long id, String comment) {
        Requirement req = get(id);
        if (!"PENDING_RECEIVE".equals(req.getStatus())) {
            throw new BizException(400, "只有待接收状态的需求可以驳回");
        }
        String fromStatus = req.getStatus();
        req.setStatus("REJECTED");
        req.setReviewComment(comment);
        requirementRepository.save(req);
        saveChangeLog(id, "status", fromStatus, "REJECTED", "驳回：" + comment);
        return req;
    }

    // ==================== 变更记录 ====================

    @Transactional
    public void changeField(Long id, String fieldName, String oldValue, String newValue, String reason) {
        saveChangeLog(id, fieldName, oldValue, newValue, reason);
    }

    private void saveChangeLog(Long requirementId, String fieldName, String oldValue,
                               String newValue, String changeReason) {
        ChangeLog log = new ChangeLog();
        log.setRequirementId(requirementId);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setChangeReason(changeReason);
        log.setChangedBy(getCurrentUser());
        log.setCreatedAt(Instant.now());
        changeLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ChangeLog> changeLogs(Long id) {
        return changeLogRepository.findByRequirementIdOrderByCreatedAtDesc(id);
    }

    // ==================== 审批记录 ====================

    @Transactional(readOnly = true)
    public List<ApprovalRecord> approvalRecords(Long id) {
        return approvalRecordRepository.findByRequirementIdOrderByCreatedAtDesc(id);
    }

    // ==================== 进度 ====================

    @Transactional
    public Requirement updateProgress(Long id, Integer progress) {
        Requirement req = get(id);
        if (progress == null || progress < 0 || progress > 100) {
            throw new BizException(400, "进度值必须在0-100之间");
        }
        req.setProgress(progress);
        List<Task> tasks = taskRepository.findByRequirementIdOrderBySortOrderAsc(id);
        if (!tasks.isEmpty()) {
            int totalProgress = 0;
            for (Task task : tasks) {
                totalProgress += task.getProgress() != null ? task.getProgress() : 0;
            }
            int avgProgress = totalProgress / tasks.size();
            req.setProgress(avgProgress);
        }
        return requirementRepository.save(req);
    }

    // ==================== 统计 ====================

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        String[] statuses = {"DRAFT", "PENDING_RECEIVE", "PENDING_PLAN", "IN_DEVELOPMENT",
                "IN_TESTING", "PENDING_RELEASE", "COMPLETED", "REJECTED"};
        Map<String, Long> statusCounts = new HashMap<>();
        for (String status : statuses) {
            statusCounts.put(status, (long) requirementRepository.findByStatus(status).size());
        }
        result.put("statusCounts", statusCounts);

        LocalDate today = LocalDate.now();
        long delayed = requirementRepository.findAll((Specification<Requirement>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("plannedEndDate")));
            predicates.add(cb.lessThan(root.get("plannedEndDate"), today));
            predicates.add(cb.not(root.get("status").in("COMPLETED", "REJECTED")));
            return cb.and(predicates.toArray(new Predicate[0]));
        }).size();
        result.put("delayedCount", delayed);

        return result;
    }
}
