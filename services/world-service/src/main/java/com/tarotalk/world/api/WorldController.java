package com.tarotalk.world.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.world.domain.AgentGoal;
import com.tarotalk.world.domain.WorldBranchScenario;
import com.tarotalk.world.domain.MemoryItem;
import com.tarotalk.world.domain.WorldCausalEdge;
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

    @PostMapping("/{worldId}/memories/compile")
    public ApiResponse<MemoryCompileResponse> compileMemories(@PathVariable UUID worldId,
                                                              @RequestParam(required = false) String ownerId,
                                                              @RequestParam(required = false) Integer limit,
                                                              @RequestParam(required = false) Double minSalience) {
        WorldService.MemoryCompileResult result = worldService.compileMemories(worldId, ownerId, limit, minSalience);
        List<MemoryItemResponse> memories = result.getMemories().stream()
                .map(MemoryItemResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.ok(new MemoryCompileResponse(
                result.getWorldId(),
                result.getOwnerId(),
                result.getCreatedCount(),
                result.getDeduplicatedCount(),
                memories.size(),
                result.getCompiledAt(),
                memories
        ));
    }

    @GetMapping("/{worldId}/memories")
    public ApiResponse<List<MemoryItemResponse>> listMemories(@PathVariable UUID worldId,
                                                              @RequestParam(required = false) String ownerId,
                                                              @RequestParam(required = false) Integer limit,
                                                              @RequestParam(required = false) Double minSalience) {
        List<MemoryItem> memories = worldService.listMemories(worldId, ownerId, limit, minSalience);
        return ApiResponse.ok(memories.stream().map(MemoryItemResponse::from).collect(Collectors.toList()));
    }

    @PostMapping("/{worldId}/causal/build")
    public ApiResponse<List<CausalEdgeResponse>> buildCausalEdges(@PathVariable UUID worldId,
                                                                  @RequestParam(required = false) String traceId) {
        List<WorldCausalEdge> edges = worldService.buildCausalGraph(worldId, traceId);
        return ApiResponse.ok(edges.stream().map(CausalEdgeResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/{worldId}/causal")
    public ApiResponse<List<CausalEdgeResponse>> listCausalEdges(@PathVariable UUID worldId,
                                                                 @RequestParam(required = false) String rootEventId,
                                                                 @RequestParam(required = false) Integer depth) {
        List<WorldCausalEdge> edges = worldService.listCausalEdges(worldId, rootEventId, depth);
        return ApiResponse.ok(edges.stream().map(CausalEdgeResponse::from).collect(Collectors.toList()));
    }

    @PostMapping("/{worldId}/branches")
    public ApiResponse<List<BranchScenarioResponse>> saveBranchScenarios(@PathVariable UUID worldId,
                                                                         @Valid @RequestBody BranchScenarioRequest request) {
        List<WorldBranchScenario> rows = worldService.saveBranchScenarios(worldId, request);
        return ApiResponse.ok(rows.stream().map(BranchScenarioResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/{worldId}/branches")
    public ApiResponse<List<BranchScenarioResponse>> listBranchScenarios(@PathVariable UUID worldId,
                                                                         @RequestParam(required = false) Integer limit) {
        List<WorldBranchScenario> rows = worldService.listBranchScenarios(worldId, limit);
        return ApiResponse.ok(rows.stream().map(BranchScenarioResponse::from).collect(Collectors.toList()));
    }
}
