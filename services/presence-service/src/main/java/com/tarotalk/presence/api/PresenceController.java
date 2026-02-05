package com.tarotalk.presence.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.presence.service.PresenceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/presence")
@Validated
public class PresenceController {
    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/heartbeat")
    public ApiResponse<PresenceResponse> heartbeat(@Valid @RequestBody HeartbeatRequest request) {
        return ApiResponse.ok(presenceService.heartbeat(request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<PresenceResponse> get(@PathVariable UUID userId) {
        return ApiResponse.ok(presenceService.getPresence(userId));
    }
}
