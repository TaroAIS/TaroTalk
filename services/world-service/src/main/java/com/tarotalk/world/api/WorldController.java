package com.tarotalk.world.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.world.domain.AgentGoal;
import com.tarotalk.world.domain.WorldEvent;
import com.tarotalk.world.service.WorldService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/worlds")
@Validated
public class WorldController {
    private final WorldService worldService;

    public WorldController(WorldService worldService) {
        this.worldService = worldService;
    }

    @GetMapping("/{worldId}/state")
    public ApiResponse<WorldStateResponse> state(@PathVariable UUID worldId) {
        return ApiResponse.ok(WorldStateResponse.from(worldService.getState(worldId)));
    }

    @PostMapping("/{worldId}/events")
    public ApiResponse<WorldEventResponse> createEvent(@PathVariable UUID worldId,
                                                       @Valid @RequestBody WorldEventRequest request) {
        WorldEvent created = worldService.appendEvent(worldId, request);
        return ApiResponse.ok(WorldEventResponse.from(created));
    }

    @GetMapping("/{worldId}/timeline")
    public ApiResponse<List<WorldEventResponse>> timeline(@PathVariable UUID worldId,
                                                          @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(worldService.timeline(worldId, limit)
                .stream()
                .map(WorldEventResponse::from)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{worldId}/goals/recompute")
    public ApiResponse<RecomputeGoalsResponse> recompute(@PathVariable UUID worldId) {
        List<AgentGoal> goals = worldService.recomputeGoals(worldId);
        List<GoalResponse> responses = goals.stream().map(GoalResponse::from).collect(Collectors.toList());
        return ApiResponse.ok(new RecomputeGoalsResponse(worldId, Instant.now(), responses));
    }
}
