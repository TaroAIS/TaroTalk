package com.tarotalk.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.world.api.WorldEventRequest;
import com.tarotalk.world.domain.MemoryItem;
import com.tarotalk.world.domain.WorldBranchScenario;
import com.tarotalk.world.domain.StoryArc;
import com.tarotalk.world.domain.WorldCausalEdge;
import com.tarotalk.world.domain.WorldEvent;
import com.tarotalk.world.domain.WorldState;
import com.tarotalk.world.repo.AgentGoalRepository;
import com.tarotalk.world.repo.MemoryItemRepository;
import com.tarotalk.world.repo.StoryArcRepository;
import com.tarotalk.world.repo.WorldBranchScenarioRepository;
import com.tarotalk.world.repo.WorldCausalEdgeRepository;
import com.tarotalk.world.repo.WorldEventRepository;
import com.tarotalk.world.repo.WorldStateRepository;
import com.tarotalk.world.service.WorldService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class WorldServiceTest {
    @Test
    void appendEventUpdatesWorldStateVersion() {
        WorldStateRepository worldStateRepository = mock(WorldStateRepository.class);
        WorldEventRepository worldEventRepository = mock(WorldEventRepository.class);
        StoryArcRepository storyArcRepository = mock(StoryArcRepository.class);
        AgentGoalRepository agentGoalRepository = mock(AgentGoalRepository.class);
        MemoryItemRepository memoryItemRepository = mock(MemoryItemRepository.class);
        WorldBranchScenarioRepository worldBranchScenarioRepository = mock(WorldBranchScenarioRepository.class);
        WorldCausalEdgeRepository worldCausalEdgeRepository = mock(WorldCausalEdgeRepository.class);

        WorldService service = new WorldService(
                worldStateRepository,
                worldEventRepository,
                storyArcRepository,
                agentGoalRepository,
                memoryItemRepository,
                worldBranchScenarioRepository,
                worldCausalEdgeRepository,
                new ObjectMapper()
        );

        UUID worldId = UUID.randomUUID();
        WorldState existing = new WorldState(worldId);
        when(worldStateRepository.findById(worldId)).thenReturn(Optional.of(existing));
        when(worldEventRepository.save(any(WorldEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storyArcRepository.findByWorldIdOrderByUpdatedAtDesc(worldId)).thenReturn(Collections.singletonList(new StoryArc(UUID.randomUUID(), worldId, "arc")));
        when(storyArcRepository.save(any(StoryArc.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(worldStateRepository.save(any(WorldState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorldEventRequest request = new WorldEventRequest();
        request.setEventType("WORLD_EVOLUTION");
        request.setActorId("director");
        request.setTraceId("trace-1");

        service.appendEvent(worldId, request);

        verify(worldEventRepository, times(1)).save(any(WorldEvent.class));
        verify(worldStateRepository, atLeastOnce()).save(any(WorldState.class));
        assertEquals(1L, existing.getVersion());
        verify(storyArcRepository, times(1)).findByWorldIdOrderByUpdatedAtDesc(eq(worldId));
    }

    @Test
    void compileMemoriesDeduplicatesByOwnerAndSourceAndSummaryHash() {
        WorldStateRepository worldStateRepository = mock(WorldStateRepository.class);
        WorldEventRepository worldEventRepository = mock(WorldEventRepository.class);
        StoryArcRepository storyArcRepository = mock(StoryArcRepository.class);
        AgentGoalRepository agentGoalRepository = mock(AgentGoalRepository.class);
        MemoryItemRepository memoryItemRepository = mock(MemoryItemRepository.class);
        WorldBranchScenarioRepository worldBranchScenarioRepository = mock(WorldBranchScenarioRepository.class);
        WorldCausalEdgeRepository worldCausalEdgeRepository = mock(WorldCausalEdgeRepository.class);

        WorldService service = new WorldService(
                worldStateRepository,
                worldEventRepository,
                storyArcRepository,
                agentGoalRepository,
                memoryItemRepository,
                worldBranchScenarioRepository,
                worldCausalEdgeRepository,
                new ObjectMapper()
        );

        UUID worldId = UUID.randomUUID();
        WorldState state = new WorldState(worldId);
        when(worldStateRepository.findById(worldId)).thenReturn(Optional.of(state));

        WorldEvent event = new WorldEvent(UUID.randomUUID(), worldId, "WORLD_EVOLUTION");
        event.setActorId("u1");
        event.setPayloadJson("{\"summary\":\"alice posted\"}");
        when(worldEventRepository.findByWorldIdOrderByCreatedAtDesc(eq(worldId), any(Pageable.class)))
                .thenReturn(Collections.singletonList(event));

        List<MemoryItem> store = new ArrayList<>();
        when(memoryItemRepository.save(any(MemoryItem.class))).thenAnswer(invocation -> {
            MemoryItem saved = invocation.getArgument(0);
            store.removeIf(item -> item.getMemoryId().equals(saved.getMemoryId()));
            store.add(saved);
            return saved;
        });
        when(memoryItemRepository.findFirstByWorldIdAndOwnerIdAndSourceEventIdAndSummaryHash(
                eq(worldId), any(String.class), any(String.class), any(String.class)))
                .thenAnswer(invocation -> {
                    String owner = invocation.getArgument(1);
                    String sourceEventId = invocation.getArgument(2);
                    String summaryHash = invocation.getArgument(3);
                    return store.stream()
                            .filter(item -> worldId.equals(item.getWorldId()))
                            .filter(item -> owner.equals(item.getOwnerId()))
                            .filter(item -> sourceEventId.equals(item.getSourceEventId()))
                            .filter(item -> summaryHash.equals(item.getSummaryHash()))
                            .findFirst();
                });
        when(memoryItemRepository.findByWorldIdAndOwnerIdOrderByUpdatedAtDesc(eq(worldId), any(String.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    String owner = invocation.getArgument(1);
                    List<MemoryItem> rows = new ArrayList<>();
                    for (MemoryItem item : store) {
                        if (worldId.equals(item.getWorldId()) && owner.equals(item.getOwnerId())) {
                            rows.add(item);
                        }
                    }
                    return rows;
                });
        when(memoryItemRepository.findByWorldIdOrderByUpdatedAtDesc(eq(worldId), any(Pageable.class)))
                .thenAnswer(invocation -> new ArrayList<>(store));

        WorldService.MemoryCompileResult first = service.compileMemories(worldId, "u1", 10, 0.05);
        WorldService.MemoryCompileResult second = service.compileMemories(worldId, "u1", 10, 0.05);

        assertEquals(1, first.getCreatedCount());
        assertEquals(0, first.getDeduplicatedCount());
        assertEquals(0, second.getCreatedCount());
        assertEquals(1, second.getDeduplicatedCount());
    }

    @Test
    void listMemoriesFiltersExpiredRows() {
        WorldStateRepository worldStateRepository = mock(WorldStateRepository.class);
        WorldEventRepository worldEventRepository = mock(WorldEventRepository.class);
        StoryArcRepository storyArcRepository = mock(StoryArcRepository.class);
        AgentGoalRepository agentGoalRepository = mock(AgentGoalRepository.class);
        MemoryItemRepository memoryItemRepository = mock(MemoryItemRepository.class);
        WorldBranchScenarioRepository worldBranchScenarioRepository = mock(WorldBranchScenarioRepository.class);
        WorldCausalEdgeRepository worldCausalEdgeRepository = mock(WorldCausalEdgeRepository.class);

        WorldService service = new WorldService(
                worldStateRepository,
                worldEventRepository,
                storyArcRepository,
                agentGoalRepository,
                memoryItemRepository,
                worldBranchScenarioRepository,
                worldCausalEdgeRepository,
                new ObjectMapper()
        );

        UUID worldId = UUID.randomUUID();
        MemoryItem valid = new MemoryItem(UUID.randomUUID(), worldId, "u1", "e1", "still useful");
        valid.setSummaryHash("hash-1");
        valid.setSalience(0.8);
        valid.setDecayRate(0.01);
        valid.setUpdatedAt(Instant.now().minusSeconds(120));
        valid.setExpiresAt(Instant.now().plusSeconds(3600));

        MemoryItem expired = new MemoryItem(UUID.randomUUID(), worldId, "u1", "e2", "old");
        expired.setSummaryHash("hash-2");
        expired.setSalience(0.9);
        expired.setDecayRate(0.01);
        expired.setUpdatedAt(Instant.now().minusSeconds(120));
        expired.setExpiresAt(Instant.now().minusSeconds(10));

        when(memoryItemRepository.findByWorldIdOrderByUpdatedAtDesc(eq(worldId), any(Pageable.class)))
                .thenReturn(java.util.Arrays.asList(valid, expired));
        when(memoryItemRepository.save(any(MemoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<MemoryItem> rows = service.listMemories(worldId, null, 10, 0.05);
        assertEquals(1, rows.size());
        assertTrue(rows.get(0).getSummary().contains("still useful"));
    }

    @Test
    void buildCausalGraphCreatesEdgesWithinSameTrace() {
        WorldStateRepository worldStateRepository = mock(WorldStateRepository.class);
        WorldEventRepository worldEventRepository = mock(WorldEventRepository.class);
        StoryArcRepository storyArcRepository = mock(StoryArcRepository.class);
        AgentGoalRepository agentGoalRepository = mock(AgentGoalRepository.class);
        MemoryItemRepository memoryItemRepository = mock(MemoryItemRepository.class);
        WorldBranchScenarioRepository worldBranchScenarioRepository = mock(WorldBranchScenarioRepository.class);
        WorldCausalEdgeRepository worldCausalEdgeRepository = mock(WorldCausalEdgeRepository.class);

        WorldService service = new WorldService(
                worldStateRepository,
                worldEventRepository,
                storyArcRepository,
                agentGoalRepository,
                memoryItemRepository,
                worldBranchScenarioRepository,
                worldCausalEdgeRepository,
                new ObjectMapper()
        );

        UUID worldId = UUID.randomUUID();
        WorldEvent e1 = new WorldEvent(UUID.randomUUID(), worldId, "E1");
        e1.setActorId("u1");
        e1.setTraceId("trace-1");
        WorldEvent e2 = new WorldEvent(UUID.randomUUID(), worldId, "E2");
        e2.setActorId("u1");
        e2.setTraceId("trace-1");
        when(worldEventRepository.findByWorldIdAndTraceIdOrderByCreatedAtAsc(worldId, "trace-1"))
                .thenReturn(java.util.Arrays.asList(e1, e2));
        when(worldCausalEdgeRepository.findFirstByWorldIdAndCauseEventIdAndEffectEventIdAndRelationType(
                eq(worldId), any(String.class), any(String.class), eq("STATE_EFFECT")))
                .thenReturn(Optional.empty());
        when(worldCausalEdgeRepository.save(any(WorldCausalEdge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<WorldCausalEdge> edges = service.buildCausalGraph(worldId, "trace-1");
        assertEquals(1, edges.size());
        assertEquals(e1.getEventId().toString(), edges.get(0).getCauseEventId());
        assertEquals(e2.getEventId().toString(), edges.get(0).getEffectEventId());
    }

    @Test
    void saveBranchScenariosPersistsDryRunBranchesWithoutTouchingWorldState() {
        WorldStateRepository worldStateRepository = mock(WorldStateRepository.class);
        WorldEventRepository worldEventRepository = mock(WorldEventRepository.class);
        StoryArcRepository storyArcRepository = mock(StoryArcRepository.class);
        AgentGoalRepository agentGoalRepository = mock(AgentGoalRepository.class);
        MemoryItemRepository memoryItemRepository = mock(MemoryItemRepository.class);
        WorldBranchScenarioRepository worldBranchScenarioRepository = mock(WorldBranchScenarioRepository.class);
        WorldCausalEdgeRepository worldCausalEdgeRepository = mock(WorldCausalEdgeRepository.class);

        WorldService service = new WorldService(
                worldStateRepository,
                worldEventRepository,
                storyArcRepository,
                agentGoalRepository,
                memoryItemRepository,
                worldBranchScenarioRepository,
                worldCausalEdgeRepository,
                new ObjectMapper()
        );

        UUID worldId = UUID.randomUUID();
        when(worldStateRepository.findById(worldId)).thenReturn(Optional.of(new WorldState(worldId)));
        when(worldBranchScenarioRepository.save(any(WorldBranchScenario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        com.tarotalk.world.api.BranchScenarioRequest request = new com.tarotalk.world.api.BranchScenarioRequest();
        request.setTraceId("trace-what-if");
        request.setSelectionPolicy("max_score");
        request.setDryRun(Boolean.TRUE);
        com.tarotalk.world.api.BranchScenarioRequest.BranchInput branch = new com.tarotalk.world.api.BranchScenarioRequest.BranchInput();
        branch.setBranchId("branch-1");
        branch.setScore(0.88);
        branch.setReason("best continuity");
        branch.setEvents(java.util.Arrays.<Map<String, Object>>asList(java.util.Collections.<String, Object>singletonMap("event", "A")));
        request.setBranches(java.util.Collections.singletonList(branch));

        List<WorldBranchScenario> rows = service.saveBranchScenarios(worldId, request);
        assertEquals(1, rows.size());
        assertEquals(Boolean.TRUE, rows.get(0).getDryRun());
        verify(worldStateRepository, never()).save(any(WorldState.class));
    }

    @Test
    void evaluateGoalEconomyDeweightsExhaustedBudget() {
        WorldStateRepository worldStateRepository = mock(WorldStateRepository.class);
        WorldEventRepository worldEventRepository = mock(WorldEventRepository.class);
        StoryArcRepository storyArcRepository = mock(StoryArcRepository.class);
        AgentGoalRepository agentGoalRepository = mock(AgentGoalRepository.class);
        MemoryItemRepository memoryItemRepository = mock(MemoryItemRepository.class);
        WorldBranchScenarioRepository worldBranchScenarioRepository = mock(WorldBranchScenarioRepository.class);
        WorldCausalEdgeRepository worldCausalEdgeRepository = mock(WorldCausalEdgeRepository.class);

        WorldService service = new WorldService(
                worldStateRepository,
                worldEventRepository,
                storyArcRepository,
                agentGoalRepository,
                memoryItemRepository,
                worldBranchScenarioRepository,
                worldCausalEdgeRepository,
                new ObjectMapper()
        );

        UUID worldId = UUID.randomUUID();
        when(worldStateRepository.findById(worldId)).thenReturn(Optional.of(new WorldState(worldId)));

        com.tarotalk.world.domain.AgentGoal exhausted = new com.tarotalk.world.domain.AgentGoal(
                UUID.randomUUID(), worldId, "agent-low-budget", "maintain_continuity", 90);
        exhausted.setScore(0.9);
        exhausted.setExpectedReward(0.9);
        exhausted.setRiskPenalty(0.1);
        exhausted.setMomentum(0.8);
        exhausted.setBudget(0.0);

        com.tarotalk.world.domain.AgentGoal healthy = new com.tarotalk.world.domain.AgentGoal(
                UUID.randomUUID(), worldId, "agent-healthy", "maintain_continuity", 80);
        healthy.setScore(0.7);
        healthy.setExpectedReward(0.7);
        healthy.setRiskPenalty(0.1);
        healthy.setMomentum(0.6);
        healthy.setBudget(1.0);

        when(agentGoalRepository.findByWorldId(worldId)).thenReturn(java.util.Arrays.asList(exhausted, healthy));

        List<WorldService.GoalEconomyEntry> rows = service.evaluateGoalEconomy(worldId, null, "maintain continuity");
        Map<String, Double> utilityByAgent = new java.util.HashMap<>();
        for (WorldService.GoalEconomyEntry row : rows) {
            utilityByAgent.put(row.getAgentId(), row.getUtility());
        }

        assertTrue(utilityByAgent.get("agent-healthy") > utilityByAgent.get("agent-low-budget"));
    }

    @Test
    void compileMemoriesFallsBackToLegacyEventQueryWhenWorldScopedEventLogIsEmpty() {
        WorldStateRepository worldStateRepository = mock(WorldStateRepository.class);
        WorldEventRepository worldEventRepository = mock(WorldEventRepository.class);
        StoryArcRepository storyArcRepository = mock(StoryArcRepository.class);
        AgentGoalRepository agentGoalRepository = mock(AgentGoalRepository.class);
        MemoryItemRepository memoryItemRepository = mock(MemoryItemRepository.class);
        WorldBranchScenarioRepository worldBranchScenarioRepository = mock(WorldBranchScenarioRepository.class);
        WorldCausalEdgeRepository worldCausalEdgeRepository = mock(WorldCausalEdgeRepository.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        WorldService service = new WorldService(
                worldStateRepository,
                worldEventRepository,
                storyArcRepository,
                agentGoalRepository,
                memoryItemRepository,
                worldBranchScenarioRepository,
                worldCausalEdgeRepository,
                new ObjectMapper(),
                restTemplate,
                "http://event"
        );

        UUID worldId = UUID.randomUUID();
        when(worldStateRepository.findById(worldId)).thenReturn(Optional.of(new WorldState(worldId)));
        when(worldEventRepository.findByWorldIdOrderByCreatedAtDesc(eq(worldId), any(Pageable.class)))
                .thenReturn(Collections.<WorldEvent>emptyList());

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url.contains("entityType=WORLD") && url.contains("entityId=" + worldId.toString())) {
                return Collections.<String, Object>singletonMap("data", Collections.emptyList());
            }
            Map<String, Object> fallbackEvent = new java.util.HashMap<>();
            fallbackEvent.put("eventId", "evt-1");
            fallbackEvent.put("eventType", "CHAT_MESSAGE_CREATED");
            fallbackEvent.put("actorId", "u1");
            fallbackEvent.put("payloadJson", "{\"summary\":\"fallback memory\"}");
            return Collections.<String, Object>singletonMap("data", Collections.singletonList(fallbackEvent));
        });

        List<MemoryItem> store = new ArrayList<>();
        when(memoryItemRepository.save(any(MemoryItem.class))).thenAnswer(invocation -> {
            MemoryItem saved = invocation.getArgument(0);
            store.removeIf(item -> item.getMemoryId().equals(saved.getMemoryId()));
            store.add(saved);
            return saved;
        });
        when(memoryItemRepository.findFirstByWorldIdAndOwnerIdAndSourceEventIdAndSummaryHash(
                eq(worldId), any(String.class), any(String.class), any(String.class)))
                .thenAnswer(invocation -> {
                    String owner = invocation.getArgument(1);
                    String sourceEventId = invocation.getArgument(2);
                    String summaryHash = invocation.getArgument(3);
                    return store.stream()
                            .filter(item -> worldId.equals(item.getWorldId()))
                            .filter(item -> owner.equals(item.getOwnerId()))
                            .filter(item -> sourceEventId.equals(item.getSourceEventId()))
                            .filter(item -> summaryHash.equals(item.getSummaryHash()))
                            .findFirst();
                });
        when(memoryItemRepository.findByWorldIdAndOwnerIdOrderByUpdatedAtDesc(eq(worldId), any(String.class), any(Pageable.class)))
                .thenAnswer(invocation -> new ArrayList<>(store));

        WorldService.MemoryCompileResult result = service.compileMemories(worldId, "u1", 10, 0.05);

        assertEquals(1, result.getCreatedCount());
        verify(restTemplate, atLeast(2)).getForObject(anyString(), eq(Map.class));
    }
}

