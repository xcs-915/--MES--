package com.itrm.platform.security;

import com.itrm.platform.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-directory")
public class UserDirectoryController {
    private final ItrmUserRepository userRepository;

    public UserDirectoryController(ItrmUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/receivers")
    @PreAuthorize("hasAuthority('requirement:assign-receiver')")
    public ApiResponse<List<Map<String, Object>>> receivers() {
        return ApiResponse.ok(activeUsers(new HashSet<>(Arrays.asList("IT_REVIEWER", "THIRD_PARTY"))));
    }

    @GetMapping("/developers")
    @PreAuthorize("hasAnyAuthority('requirement:plan','task:assign','task:create')")
    public ApiResponse<List<Map<String, Object>>> developers() {
        return ApiResponse.ok(activeUsers(new HashSet<>(Arrays.asList("DEVELOPER", "IT_APPROVER_DEVELOPER", "THIRD_PARTY"))));
    }

    private List<Map<String, Object>> activeUsers(Set<String> roles) {
        return userRepository.findAll().stream()
                .filter(user -> "ACTIVE".equals(user.getStatus()) && roles.contains(user.getRole()))
                .sorted(Comparator.comparing(user -> Optional.ofNullable(user.getRealName()).orElse(user.getUsername())))
                .map(user -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("username", user.getUsername());
                    item.put("realName", user.getRealName());
                    item.put("role", user.getRole());
                    item.put("department", user.getDepartment());
                    item.put("workloadCapacityHours", user.getWorkloadCapacityHours());
                    return item;
                }).collect(Collectors.toList());
    }
}
