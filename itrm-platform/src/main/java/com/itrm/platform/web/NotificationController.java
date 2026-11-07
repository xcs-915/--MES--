package com.itrm.platform.web;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.domain.Notification;
import com.itrm.platform.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public ApiResponse<List<Notification>> list(Authentication authentication) { return ApiResponse.ok(service.list(authentication.getName())); }
    @GetMapping("/unread-count") public ApiResponse<Map<String, Long>> unread(Authentication authentication) { return ApiResponse.ok(Collections.singletonMap("count", service.unread(authentication.getName()))); }
    @PutMapping("/{id}/read") public ApiResponse<Notification> read(@PathVariable Long id, Authentication authentication) { return ApiResponse.ok(service.read(id, authentication.getName())); }
}
