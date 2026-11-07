package com.itrm.platform.service;

import com.itrm.platform.common.BizException;
import com.itrm.platform.domain.Requirement;
import com.itrm.platform.repo.RequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class RequirementImportService {
    private static final Set<String> STATUSES = new HashSet<>(Arrays.asList("DRAFT","PENDING_RECEIVE","PENDING_DOCUMENT","PENDING_APPROVAL","APPROVED","IN_DEVELOPMENT","IN_TESTING","PENDING_RELEASE","COMPLETED","REJECTED"));
    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList("NEW_FUNC","FUNC_OPT","BUG","INTERFACE","REPORT","OTHER"));
    private final RequirementRepository repository;

    public RequirementImportService(RequirementRepository repository) { this.repository = repository; }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> importRequirements(Map<String, Object> request) {
        String source = text(request.get("source"));
        Object raw = request.get("requirements");
        if (!StringUtils.hasText(source) || !(raw instanceof List)) throw new BizException(400, "缺少导入来源或需求列表");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) raw;
        if (rows.isEmpty() || rows.size() > 2000) throw new BizException(400, "导入需求数量必须在 1 到 2000 之间");
        List<Map<String, Object>> mapping = new ArrayList<>();
        int created = 0;
        int updated = 0;
        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = rows.get(index);
            String externalRef = text(row.get("externalRef"));
            String title = text(row.get("title"));
            if (!StringUtils.hasText(externalRef) || !StringUtils.hasText(title)) throw new BizException(400, "第 " + (index + 1) + " 条缺少编号或标题");
            Requirement requirement = repository.findByImportSourceAndExternalRef(source, externalRef).orElse(null);
            if (requirement == null) {
                requirement = new Requirement();
                requirement.setReqNo(importNumber(source, externalRef));
                requirement.setImportSource(source);
                requirement.setExternalRef(externalRef);
                created++;
            } else updated++;
            requirement.setTitle(trimTo(title, 200));
            requirement.setCategory(allowed(text(row.get("category")), CATEGORIES, "OTHER"));
            requirement.setBelongSystem(allowed(text(row.get("belongSystem")), new HashSet<>(Arrays.asList("MES","WMS","SAP","OA","SCADA","OTHER")), "OTHER"));
            requirement.setUrgency(allowed(text(row.get("urgency")), new HashSet<>(Arrays.asList("URGENT","HIGH","MEDIUM","LOW")), "MEDIUM"));
            requirement.setSourceDept(trimTo(text(row.get("sourceDept")), 200));
            requirement.setProposer(trimTo(text(row.get("proposer")), 64));
            requirement.setCurrentProblem(text(row.get("currentProblem")));
            requirement.setDescription(text(row.get("description")));
            requirement.setExpectedEffect(text(row.get("expectedEffect")));
            requirement.setAcceptanceCriteria(text(row.get("acceptanceCriteria")));
            requirement.setRemark(trimTo(text(row.get("remark")), 1000));
            requirement.setLegacyStatus(trimTo(text(row.get("legacyStatus")), 100));
            requirement.setStatus(allowed(text(row.get("status")), STATUSES, "PENDING_RECEIVE"));
            requirement.setReviewOwner(trimTo(text(row.get("reviewOwner")), 64));
            requirement.setDevOwner(trimTo(text(row.get("devOwner")), 64));
            requirement.setExpectedCompletionDate(date(row.get("expectedCompletionDate")));
            requirement.setPlannedEndDate(date(row.get("plannedEndDate")));
            requirement.setOnlineDate(date(row.get("onlineDate")));
            if ("COMPLETED".equals(requirement.getStatus())) {
                requirement.setProgress(100);
                requirement.setCompleteDate(requirement.getOnlineDate() != null ? requirement.getOnlineDate() : date(row.get("completedDate")));
            } else requirement.setProgress(number(row.get("progress"), 0));
            requirement.setEstimatedHours(decimal(row.get("estimatedHours")));
            Instant sourceCreatedAt = instant(row.get("createdAt"));
            if (requirement.getId() == null && sourceCreatedAt != null) requirement.setCreatedAt(sourceCreatedAt);
            Requirement saved = repository.save(requirement);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("externalRef", externalRef); item.put("id", saved.getId()); item.put("reqNo", saved.getReqNo());
            mapping.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", source); result.put("total", rows.size()); result.put("created", created); result.put("updated", updated); result.put("mapping", mapping);
        return result;
    }

    private String importNumber(String source, String ref) {
        String prefix = source.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (prefix.length() > 8) prefix = prefix.substring(0, 8);
        String cleanRef = ref.replaceAll("[^A-Za-z0-9]", "");
        if (cleanRef.length() > 14) cleanRef = cleanRef.substring(cleanRef.length() - 14);
        String base = "LEG-" + prefix + "-" + cleanRef;
        String candidate = base.length() <= 32 ? base : base.substring(0, 32);
        int suffix = 1;
        while (repository.existsByReqNo(candidate)) {
            String ending = "-" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 32 - ending.length())) + ending;
        }
        return candidate;
    }

    private static String allowed(String value, Set<String> allowed, String fallback) { return allowed.contains(value) ? value : fallback; }
    private static String text(Object value) { return value == null ? "" : value.toString().trim(); }
    private static String trimTo(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private static LocalDate date(Object value) { try { return StringUtils.hasText(text(value)) ? LocalDate.parse(text(value).substring(0, 10)) : null; } catch (Exception ignored) { return null; } }
    private static Instant instant(Object value) { try { return StringUtils.hasText(text(value)) ? LocalDate.parse(text(value).substring(0, 10)).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant() : null; } catch (Exception ignored) { return null; } }
    private static BigDecimal decimal(Object value) { try { return new BigDecimal(text(value)); } catch (Exception ignored) { return BigDecimal.ZERO; } }
    private static int number(Object value, int fallback) { try { return Integer.parseInt(text(value)); } catch (Exception ignored) { return fallback; } }
}
