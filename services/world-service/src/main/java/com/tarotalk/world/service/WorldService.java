package com.tarotalk.world.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.common.exception.ApiException;
import com.tarotalk.world.api.WorldEventRequest;
import com.tarotalk.world.domain.AgentGoal;
import com.tarotalk.world.domain.StoryArc;
import com.tarotalk.world.domain.WorldEvent;
import com.tarotalk.world.domain.WorldState;
import com.tarotalk.world.repo.AgentGoalRepository;
import com.tarotalk.world.repo.StoryArcRepository;
import com.tarotalk.world.repo.WorldEventRepository;
import com.tarotalk.world.repo.WorldStateRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class WorldService {
    private final WorldStateRepository worldStateRepository;
    private final WorldEventRepository worldEventRepository;
    private final StoryArcRepository storyArcRepository;
    private final AgentGoalRepository agentGoalRepository;
    private final ObjectMapper objectMapper;

    public WorldService(WorldStateRepository worldStateRepository,
                        WorldEventRepository worldEventRepository,
                        StoryArcRepository storyArcRepository,
                        AgentGoalRepository agentGoalRepository,
                        ObjectMapper objectMapper) {
        this.worldStateRepository = worldStateRepository;
        this.worldEventRepository = worldEventRepository;
        this.storyArcRepository = storyArcRepository;
        this.agentGoalRepository = agentGoalRepository;
        this.objectMapper = objectMapper;
    }

    public WorldState getState(UUID worldId) {
        return ensureWorldState(worldId);
    }

    public WorldEvent appendEvent(UUID worldId, WorldEventRequest request) {
        if (request.getEventType() == null || request.getEventType().trim().isEmpty()) {
            throw new ApiException("VALIDATION_ERROR", "eventType is required");
        }
        WorldEvent event = new WorldEvent(UUID.randomUUID(), worldId, request.getEventType().trim());
        event.setActorId(clean(request.getActorId()));
        event.setTraceId(clean(request.getTraceId()));
        event.setPayloadJson(toPayloadJson(request.getPayload()));
        event.setCreatedAt(Instant.now());
        WorldEvent saved = worldEventRepository.save(event);
        touchStoryArc(worldId, saved);
        updateWorldState(worldId, saved);
        return saved;
    }

    public List<WorldEvent> timeline(UUID worldId, Integer limit) {
        int size = resolveLimit(limit);
        return worldEventRepository.findByWorldIdOrderByCreatedAtDesc(worldId, PageRequest.of(0, size));
    }

    public List<AgentGoal> recomputeGoals(UUID worldId) {
        WorldState state = ensureWorldState(worldId);
        List<WorldEvent> latestEvents = worldEventRepository.findByWorldIdOrderByCreatedAtDesc(worldId, PageRequest.of(0, 20));
        List<AgentGoal> goals = agentGoalRepository.findByWorldId(worldId);
        if (goals.isEmpty()) {
            AgentGoal defaultGoal = new AgentGoal(UUID.randomUUID(), worldId, "director", "maintain_continuity", 100);
            defaultGoal.setScore(0.4);
            defaultGoal.setUpdatedAt(Instant.now());
            goals.add(agentGoalRepository.save(defaultGoal));
        }

        double eventDensityScore = Math.min(latestEvents.size() / 20.0, 1.0);
        double freshnessScore = 0.0;
        if (!latestEvents.isEmpty()) {
            Instant latest = latestEvents.get(0).getCreatedAt();
            long minutes = Math.max(1L, java.time.Duration.between(latest, Instant.now()).toMinutes());
            freshnessScore = Math.exp(-minutes / 180.0);
        }

        for (AgentGoal goal : goals) {
            double base = goal.getPriority() / 100.0;
            double recomputed = base * 0.5 + eventDensityScore * 0.3 + freshnessScore * 0.2;
            goal.setScore(Math.min(1.0, recomputed));
            goal.setUpdatedAt(Instant.now());
        }
        List<AgentGoal> savedGoals = agentGoalRepository.saveAll(goals);
        savedGoals.sort(new Comparator<AgentGoal>() {
            @Override
            public int compare(AgentGoal left, AgentGoal right) {
                int priorityCompare = Integer.compare(right.getPriority(), left.getPriority());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                return Double.compare(right.getScore(), left.getScore());
            }
        });

        state.setUpdatedAt(Instant.now());
        worldStateRepository.save(state);
        return savedGoals;
    }

    private void touchStoryArc(UUID worldId, WorldEvent event) {
        List<StoryArc> arcs = storyArcRepository.findByWorldIdOrderByUpdatedAtDesc(worldId);
        StoryArc active;
        if (arcs.isEmpty()) {
            active = new StoryArc(UUID.randomUUID(), worldId, "Daily social evolution");
        } else {
            active = arcs.get(0);
        }
        double nextProgress = Math.min(1.0, active.getProgress() + 0.05);
        active.setProgress(nextProgress);
        active.setStatus(nextProgress >= 1.0 ? "COMPLETED" : "ACTIVE");
        active.setMetadataJson("{\"latest_event\":\"" + event.getEventType() + "\"}");
        active.setUpdatedAt(Instant.now());
        storyArcRepository.save(active);
    }

    private void updateWorldState(UUID worldId, WorldEvent event) {
        WorldState state = ensureWorldState(worldId);
        state.setVersion(state.getVersion() + 1);
        state.setUpdatedAt(Instant.now());
        state.setSnapshotJson("{\"last_event_id\":\"" + event.getEventId()
                + "\",\"last_event_type\":\"" + event.getEventType()
                + "\",\"trace_id\":\"" + safe(event.getTraceId()) + "\"}");
        worldStateRepository.save(state);
    }

    private WorldState ensureWorldState(UUID worldId) {
        return worldStateRepository.findById(worldId)
                .orElseGet(() -> worldStateRepository.save(new WorldState(worldId)));
    }

    private String toPayloadJson(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String) {
            return (String) payload;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new ApiException("SERIALIZATION_ERROR", "payload serialization failed");
        }
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 50;
        }
        return Math.min(limit, 500);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
