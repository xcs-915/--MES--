package com.itrm.platform.security;

import com.itrm.platform.common.BizException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserImportService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private final ItrmUserRepository userRepository;
    private final ItrmDepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserImportService(ItrmUserRepository userRepository,
                             ItrmDepartmentRepository departmentRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> importUsers(Map<String, Object> request) {
        String defaultPassword = text(request.get("defaultPassword"));
        if (defaultPassword.length() < 6) throw new BizException(400, "默认密码至少 6 位");
        Object rawUsers = request.get("users");
        if (!(rawUsers instanceof List)) throw new BizException(400, "缺少用户列表");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) rawUsers;
        if (rows.isEmpty() || rows.size() > 5000) throw new BizException(400, "用户数量必须在 1 到 5000 之间");

        Map<String, ItrmUser> existing = userRepository.findAll().stream()
                .collect(Collectors.toMap(ItrmUser::getUsername, user -> user, (a, b) -> a));
        Map<String, ItrmDepartment> departments = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(ItrmDepartment::getFullPath, item -> item, (a, b) -> a));
        List<ItrmUser> imported = new ArrayList<>();
        Set<String> usernames = new HashSet<>();
        String encodedPassword = passwordEncoder.encode(defaultPassword);
        int created = 0;
        int updated = 0;

        for (int index = 0; index < rows.size(); index++) {
            Map<String, Object> row = rows.get(index);
            String username = text(row.get("username"));
            String realName = text(row.get("realName"));
            String department = DepartmentAdminController.normalizePath(text(row.get("department")));
            if (!USERNAME_PATTERN.matcher(username).matches()) throw new BizException(400, "第 " + (index + 1) + " 条账号格式无效");
            if (!StringUtils.hasText(realName)) throw new BizException(400, "第 " + (index + 1) + " 条姓名为空");
            if (!StringUtils.hasText(department)) throw new BizException(400, "第 " + (index + 1) + " 条部门为空");
            if (!usernames.add(username)) throw new BizException(400, "导入账号重复：" + username);
            for (Object path : list(row.get("departmentPaths"), department)) ensureDepartmentPath(text(path), departments);

            ItrmUser user = existing.get(username);
            if (user == null) {
                user = new ItrmUser();
                user.setUsername(username);
                user.setRole(roleFor(realName));
                created++;
            } else updated++;
            user.setPassword(encodedPassword);
            user.setRealName(trimTo(realName, 64));
            user.setDepartment(trimTo(department, 64));
            user.setEmail(trimTo(text(row.get("email")), 200));
            user.setStatus(Boolean.FALSE.equals(row.get("active")) ? "DISABLED" : "ACTIVE");
            String configuredRole = roleFor(realName);
            if (!"BUSINESS_USER".equals(configuredRole)) user.setRole(configuredRole);
            imported.add(user);
        }
        departmentRepository.saveAll(departments.values());
        userRepository.saveAll(imported);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", imported.size()); result.put("created", created); result.put("updated", updated);
        result.put("departments", departments.size());
        return result;
    }

    private void ensureDepartmentPath(String rawPath, Map<String, ItrmDepartment> departments) {
        String path = DepartmentAdminController.normalizePath(rawPath);
        if (path.isEmpty()) return;
        String parent = null;
        StringBuilder current = new StringBuilder();
        for (String rawName : path.split("/")) {
            String name = rawName.trim();
            if (name.isEmpty()) continue;
            if (current.length() > 0) current.append('/');
            current.append(name);
            String fullPath = current.toString();
            if (!departments.containsKey(fullPath)) {
                ItrmDepartment item = new ItrmDepartment();
                item.setName(trimTo(name, 100)); item.setFullPath(fullPath); item.setParentPath(parent); item.setStatus("ACTIVE");
                departments.put(fullPath, item);
            }
            parent = fullPath;
        }
    }

    private static List<?> list(Object value, String fallback) {
        if (value instanceof List && !((List<?>) value).isEmpty()) return (List<?>) value;
        return Collections.singletonList(fallback);
    }
    private static String text(Object value) { return value == null ? "" : value.toString().trim(); }
    private static String trimTo(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private static String roleFor(String realName) {
        if (Arrays.asList("龚嘉俊", "蒋晓晓", "王欣怡", "刘洲").contains(realName)) return "IT_REVIEWER";
        if ("刘文斌".equals(realName)) return "IT_APPROVER";
        if ("徐昌师".equals(realName)) return "IT_APPROVER_DEVELOPER";
        if ("史浩军".equals(realName)) return "DEVELOPER";
        return "BUSINESS_USER";
    }
}
