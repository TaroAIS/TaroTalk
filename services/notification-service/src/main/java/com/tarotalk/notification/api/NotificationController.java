package com.tarotalk.notification.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.notification.service.NotificationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@RequestParam UUID userId) {
        List<NotificationResponse> notifications = notificationService.list(userId).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.ok(notifications);
    }

    @PostMapping
    public ApiResponse<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        return ApiResponse.ok(NotificationResponse.from(notificationService.create(request)));
    }
}
