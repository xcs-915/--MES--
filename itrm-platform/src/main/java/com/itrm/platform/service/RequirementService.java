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
import java.util.Objects;
import java.util.function.Consumer;

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
        throw new BizException(400, "需求创建后不允许直接编辑，请使用需求变更并填写变更原因");
    }

    @Transactional
    public Requirement updateDocument(Long id, Map<String, Object> body) {
        Requirement req = get(id);
        if (!"PENDING_DOCUMENT".equals(req.getStatus())) {
            throw new BizException(400, "只有待完善文档状态可以编辑需求文档");
        }
        String reason = requiredReason(body, "请填写本次文档完善说明");
        boolean changed = false;
        changed |= applyString(req, id, body, "currentProblem", "当前问题", req.getCurrentProblem(), req::setCurrentProblem, reason);
        changed |= applyString(req, id, body, "expectedEffect", "预期效果", req.getExpectedEffect(), req::setExpectedEffect, reason);
        changed |= applyString(req, id, body, "acceptanceCriteria", "验收标准", req.getAcceptanceCriteria(), req::setAcceptanceCriteria, reason);
        changed |= applyString(req, id, body, "feasibility", "可行性分析", req.getFeasibility(), req::setFeasibility, reason);
        changed |= applyString(req, id, body, "description", "需求说明", req.getDescription(), req::setDescription, reason);
        if (!changed) {
            saveChangeLog(id, "需求文档确认", "未确认", "已确认", reason);
        }
        ensureDocumentComplete(req);
        return requirementRepository.save(req);
    }

    @Transactional
    public Requirement change(Long id, Map<String, Object> body) {
        Requirement req = get(id);
        if ("COMPLETED".equals(req.getStatus())) {
            throw new BizException(400, "已完成需求请先重新打开后再变更");
        }
        String reason = requiredReason(body, "需求变更必须填写变更原因");
        if (body.containsKey("category") && !CATEGORIES.contains(String.valueOf(body.get("category")))) {
            throw new BizException(400, "需求类型无效");
        }
        if (body.containsKey("belongSystem") && !SYSTEMS.contains(String.valueOf(body.get("belongSystem")))) {
            throw new BizException(400, "所属系统无效");
        }
        boolean changed = false;
        changed |= applyString(req, id, body, "title", "需求标题", req.getTitle(), req::setTitle, reason);
        changed |= applyString(req, id, body, "category", "需求类型", req.getCategory(), req::setCategory, reason);
        changed |= applyString(req, id, body, "urgency", "紧急程度", req.getUrgency(), req::setUrgency, reason);
        changed |= applyString(req, id, body, "belongSystem", "所属系统", req.getBelongSystem(), req::setBelongSystem, reason);
        changed |= applyString(req, id, body, "sourceDept", "来源部门", req.getSourceDept(), req::setSourceDept, reason);
        changed |= applyString(req, id, body, "proposer", "提出人", req.getProposer(), req::setProposer, reason);
        changed |= applyString(req, id, body, "currentProblem", "当前问题", req.getCurrentProblem(), req::setCurrentProblem, reason);
        changed |= applyString(req, id, body, "expectedEffect", "预期效果", req.getExpectedEffect(), req::setExpectedEffect, reason);
        changed |= applyString(req, id, body, "acceptanceCriteria", "验收标准", req.getAcceptanceCriteria(), req::setAcceptanceCriteria, reason);
        changed |= applyString(req, id, body, "remark", "备注", req.getRemark(), req::setRemark, reason);
        if (body.containsKey("plannedEndDate")) {
            LocalDate next = body.get("plannedEndDate") == null || body.get("plannedEndDate").toString().isEmpty()
                    ? null : LocalDate.parse(body.get("plannedEndDate").toString());
            if (!Objects.equals(req.getPlannedEndDate(), next)) {
                saveChangeLog(id, "预计完成时间", String.valueOf(req.getPlannedEndDate()), String.valueOf(next), reason);
                req.setPlannedEndDate(next);
                changed = true;
            }
        }
        if (!changed) {
            throw new BizException(400, "没有检测到需求变更");
        }
        if (Arrays.asList("REJECTED", "PENDING_APPROVAL", "APPROVED", "IN_DEVELOPMENT", "IN_TESTING", "PENDING_RELEASE").contains(req.getStatus())) {
            String previousStatus = req.getStatus();
            if ("PENDING_APPROVAL".equals(previousStatus)) {
                approvalRecordRepository.findByRequirementIdOrderByCreatedAtDesc(id).stream()
                        .filter(record -> "PENDING".equals(record.getAction()))
                        .forEach(record -> {
                            record.setAction("CANCELLED");
                            record.setComment("需求发生变更，原审批自动终止");
                            record.setActedAt(Instant.now());
                            approvalRecordRepository.save(record);
                        });
            }
            req.setStatus("PENDING_DOCUMENT");
            req.setApprovalNodeIdx(0);
            saveChangeLog(id, "需求状态", previousStatus, "PENDING_DOCUMENT", "需求变更后需重新完善文档并审批：" + reason);
        }
        return requirementRepository.save(req);
    }

    private String requiredReason(Map<String, Object> body, String message) {
        String reason = body.get("reason") == null ? null : body.get("reason").toString().trim();
        if (!StringUtils.hasText(reason)) throw new BizException(400, message);
        return reason;
    }

    private boolean applyString(Requirement req, Long id, Map<String, Object> body, String key,
                                String label, String oldValue, Consumer<String> setter, String reason) {
        if (!body.containsKey(key)) return false;
        String next = body.get(key) == null ? null : body.get(key).toString().trim();
        if (Objects.equals(oldValue, next)) return false;
        setter.accept(next);
        saveChangeLog(id, label, oldValue, next, reason);
        return true;
    }

    private void ensureDocumentComplete(Requirement req) {
        if (!StringUtils.hasText(req.getCurrentProblem()) || !StringUtils.hasText(req.getExpectedEffect())
                || !StringUtils.hasText(req.getAcceptanceCriteria())) {
            throw new BizException(400, "需求文档必须包含当前问题、预期效果和验收标准");
        }
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
    public Requirement receive(Long id, String reviewOwner, Long templateId, LocalDate plannedEndDate) {
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
        if (plannedEndDate == null) {
            throw new BizException(400, "预计完成时间不能为空");
        }

        ApprovalTemplate template = approvalTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BizException(404, "审批模板不存在"));
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BizException(400, "审批模板未启用");
        }

        String fromStatus = req.getStatus();
        req.setStatus("PENDING_DOCUMENT");
        req.setReviewOwner(reviewOwner);
        req.setReceivedAt(Instant.now());
        req.setApprovalTemplateId(templateId);
        req.setApprovalNodeIdx(0);
        req.setPlannedEndDate(plannedEndDate);
        requirementRepository.save(req);
        saveChangeLog(id, "预计完成时间", null, plannedEndDate.toString(), "IT接收需求时评估");
        saveChangeLog(id, "需求状态", fromStatus, "PENDING_DOCUMENT", "IT接收需求，待完善需求文档");
        return req;
    }

    @Transactional
    public Requirement submitApproval(Long id) {
        Requirement req = get(id);
        if (!"PENDING_DOCUMENT".equals(req.getStatus())) {
            throw new BizException(400, "只有待完善文档状态可以提交审批");
        }
        ensureDocumentComplete(req);
        if (req.getPlannedEndDate() == null) throw new BizException(400, "请先填写预计完成时间");
        if (req.getApprovalTemplateId() == null) throw new BizException(400, "请先选择审批流");
        List<ApprovalNode> nodes = approvalNodeRepository.findByTemplateIdOrderBySortOrderAsc(req.getApprovalTemplateId());
        if (nodes.isEmpty()) throw new BizException(400, "审批模板未配置节点");
        boolean hasPending = approvalRecordRepository.findByRequirementIdOrderByCreatedAtDesc(id).stream()
                .anyMatch(record -> "PENDING".equals(record.getAction()));
        if (!hasPending) {
            ApprovalNode firstNode = nodes.get(0);
            ApprovalRecord record = new ApprovalRecord();
            record.setRequirementId(id);
            record.setTemplateId(req.getApprovalTemplateId());
            record.setNodeIdx(0);
            record.setNodeName(firstNode.getNodeName());
            record.setNodeType(firstNode.getNodeType());
            record.setAction("PENDING");
            record.setCreatedAt(Instant.now());
            approvalRecordRepository.save(record);
        }
        req.setApprovalNodeIdx(0);
        req.setStatus("PENDING_APPROVAL");
        requirementRepository.save(req);
        saveChangeLog(id, "需求状态", "PENDING_DOCUMENT", "PENDING_APPROVAL", "需求文档完成，提交上级审批");
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
        if (!"PENDING_APPROVAL".equals(req.getStatus())) {
            throw new BizException(400, "只有待审批状态的需求可以审批");
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
                // 最后一个审批节点，进入待排期阶段
                req.setStatus("APPROVED");
                req.setApprovalNodeIdx(nextIdx);
                requirementRepository.save(req);
                saveChangeLog(id, "需求状态", fromStatus, "APPROVED", "审批通过，待排期分配");
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
        if (!"APPROVED".equals(req.getStatus())) {
            throw new BizException(400, "只有审批通过的需求可以排期");
        }
        if (!StringUtils.hasText(devOwner)) {
            throw new BizException(400, "开发负责人不能为空");
        }
        String fromStatus = req.getStatus();
        req.setStatus("IN_DEVELOPMENT");
        req.setDevOwner(devOwner);
        req.setPlannedStartDate(plannedStart);
        req.setPlannedEndDate(plannedEnd != null ? plannedEnd : req.getPlannedEndDate());
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
        String[] statuses = {"DRAFT", "PENDING_RECEIVE", "PENDING_DOCUMENT", "PENDING_APPROVAL", "APPROVED",
                "IN_DEVELOPMENT", "IN_TESTING", "PENDING_RELEASE", "COMPLETED", "REJECTED"};
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
