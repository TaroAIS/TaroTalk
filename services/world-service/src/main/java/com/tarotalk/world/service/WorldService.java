package com.tarotalk.world.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.common.exception.ApiException;
import com.tarotalk.world.api.BranchScenarioRequest;
import com.tarotalk.world.api.WorldEventRequest;
import com.tarotalk.world.domain.AgentGoal;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class WorldService {
    private static final double MEMORY_MIN_SALIENCE = 0.05;
    private static final double DEFAULT_MEMORY_DECAY_RATE = 0.05;

    private final WorldStateRepository worldStateRepository;
    private final WorldEventRepository worldEventRepository;
    private final StoryArcRepository storyArcRepository;
    private final AgentGoalRepository agentGoalRepository;
    private final MemoryItemRepository memoryItemRepository;
    private final WorldBranchScenarioRepository worldBranchScenarioRepository;
    private final WorldCausalEdgeRepository worldCausalEdgeRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String eventServiceUrl;

    public WorldService(WorldStateRepository worldStateRepository,
                        WorldEventRepository worldEventRepository,
                        StoryArcRepository storyArcRepository,
                        AgentGoalRepository agentGoalRepository,
                        MemoryItemRepository memoryItemRepository,
                        WorldBranchScenarioRepository worldBranchScenarioRepository,
                        WorldCausalEdgeRepository worldCausalEdgeRepository,
                        ObjectMapper objectMapper,
                        RestTemplate restTemplate,
                        @Value("${integrations.event-service.base-url:}") String eventServiceUrl) {
        this.worldStateRepository = worldStateRepository;
        this.worldEventRepository = worldEventRepository;
        this.storyArcRepository = storyArcRepository;
        this.agentGoalRepository = agentGoalRepository;
        this.memoryItemRepository = memoryItemRepository;
        this.worldBranchScenarioRepository = worldBranchScenarioRepository;
        this.worldCausalEdgeRepository = worldCausalEdgeRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.eventServiceUrl = eventServiceUrl;
    }

    public WorldService(WorldStateRepository worldStateRepository,
                        WorldEventRepository worldEventRepository,
                        StoryArcRepository storyArcRepository,
                        AgentGoalRepository agentGoalRepository,
                        MemoryItemRepository memoryItemRepository,
                        WorldBranchScenarioRepository worldBranchScenarioRepository,
                        WorldCausalEdgeRepository worldCausalEdgeRepository,
                        ObjectMapper objectMapper) {
        this(worldStateRepository,
                worldEventRepository,
                storyArcRepository,
                agentGoalRepository,
                memoryItemRepository,
                worldBranchScenarioRepository,
                worldCausalEdgeRepository,
                objectMapper,
                null,
                "");
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
            long minutes = Math.max(1L, Duration.between(latest, Instant.now()).toMinutes());
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

    public List<WorldCausalEdge> buildCausalGraph(UUID worldId, String traceId) {
        String cleanedTrace = clean(traceId);
        List<WorldEvent> events;
        if (cleanedTrace != null) {
            events = worldEventRepository.findByWorldIdAndTraceIdOrderByCreatedAtAsc(worldId, cleanedTrace);
        } else {
            List<WorldEvent> latest = worldEventRepository.findByWorldIdOrderByCreatedAtDesc(worldId, PageRequest.of(0, 100));
            events = new ArrayList<>(latest);
            events.sort(new Comparator<WorldEvent>() {
                @Override
                public int compare(WorldEvent left, WorldEvent right) {
                    Instant leftCreated = left.getCreatedAt() == null ? Instant.EPOCH : left.getCreatedAt();
                    Instant rightCreated = right.getCreatedAt() == null ? Instant.EPOCH : right.getCreatedAt();
                    return leftCreated.compareTo(rightCreated);
                }
            });
        }

        if (events.size() < 2) {
            return new ArrayList<>();
        }

        List<WorldCausalEdge> built = new ArrayList<>();
        for (int i = 0; i < events.size() - 1; i++) {
            WorldEvent cause = events.get(i);
            WorldEvent effect = events.get(i + 1);
            if (cause.getEventId() == null || effect.getEventId() == null) {
                continue;
            }
            String causeEventId = cause.getEventId().toString();
            String effectEventId = effect.getEventId().toString();
            String relationType = "STATE_EFFECT";
            Optional<WorldCausalEdge> existing = worldCausalEdgeRepository
                    .findFirstByWorldIdAndCauseEventIdAndEffectEventIdAndRelationType(
                            worldId,
                            causeEventId,
                            effectEventId,
                            relationType
                    );
            if (existing.isPresent()) {
                built.add(existing.get());
                continue;
            }
            double confidence = 0.65;
            if (cause.getActorId() != null && cause.getActorId().equals(effect.getActorId())) {
                confidence += 0.15;
            }
            WorldCausalEdge edge = new WorldCausalEdge(
                    UUID.randomUUID(),
                    worldId,
                    causeEventId,
                    effectEventId,
                    relationType,
                    clamp01(confidence),
                    cleanedTrace == null ? safe(effect.getTraceId()) : cleanedTrace
            );
            edge.setCreatedAt(Instant.now());
            built.add(worldCausalEdgeRepository.save(edge));
        }
        return built;
    }

    public List<WorldCausalEdge> listCausalEdges(UUID worldId, String rootEventId, Integer depth) {
        String root = clean(rootEventId);
        if (root == null) {
            return worldCausalEdgeRepository.findByWorldIdOrderByCreatedAtDesc(worldId, PageRequest.of(0, 100));
        }
        int maxDepth = resolveDepth(depth);
        Set<String> frontier = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        frontier.add(root);
        List<WorldCausalEdge> rows = new ArrayList<>();

        for (int level = 0; level < maxDepth; level++) {
            if (frontier.isEmpty()) {
                break;
            }
            Set<String> nextFrontier = new LinkedHashSet<>();
            for (String causeEventId : frontier) {
                if (causeEventId == null || visited.contains(causeEventId)) {
                    continue;
                }
                visited.add(causeEventId);
                List<WorldCausalEdge> edges = worldCausalEdgeRepository
                        .findByWorldIdAndCauseEventIdOrderByCreatedAtAsc(worldId, causeEventId);
                for (WorldCausalEdge edge : edges) {
                    rows.add(edge);
                    if (edge.getEffectEventId() != null && !visited.contains(edge.getEffectEventId())) {
                        nextFrontier.add(edge.getEffectEventId());
                    }
                }
            }
            frontier = nextFrontier;
        }
        return rows;
    }

    public List<WorldBranchScenario> saveBranchScenarios(UUID worldId, BranchScenarioRequest request) {
        ensureWorldState(worldId);
        if (request == null || request.getBranches() == null || request.getBranches().isEmpty()) {
            throw new ApiException("VALIDATION_ERROR", "branches are required");
        }
        String traceId = clean(request.getTraceId());
        String selectionPolicy = clean(request.getSelectionPolicy());
        Boolean dryRun = request.getDryRun() == null ? Boolean.TRUE : request.getDryRun();
        List<WorldBranchScenario> rows = new ArrayList<>();
        for (BranchScenarioRequest.BranchInput branch : request.getBranches()) {
            if (branch == null || branch.getBranchId() == null || branch.getBranchId().trim().isEmpty()) {
                continue;
            }
            WorldBranchScenario scenario = new WorldBranchScenario(
                    UUID.randomUUID(),
                    worldId,
                    branch.getBranchId().trim(),
                    branch.getScore() == null ? 0.0 : branch.getScore()
            );
            scenario.setTraceId(traceId);
            scenario.setSelectionPolicy(selectionPolicy);
            scenario.setReason(clean(branch.getReason()));
            scenario.setEventsJson(toPayloadJson(branch.getEvents()));
            scenario.setDryRun(dryRun);
            scenario.setCreatedAt(Instant.now());
            rows.add(worldBranchScenarioRepository.save(scenario));
        }
        return rows;
    }

    public List<WorldBranchScenario> listBranchScenarios(UUID worldId, Integer limit) {
        int size = resolveLimit(limit);
        return worldBranchScenarioRepository.findByWorldIdOrderByCreatedAtDesc(worldId, PageRequest.of(0, size));
    }

    public MemoryCompileResult compileMemories(UUID worldId, String ownerId, Integer limit, Double minSalience) {
        ensureWorldState(worldId);
        String owner = clean(ownerId);
        int size = resolveMemoryLimit(limit);
        double min = normalizeMinSalience(minSalience);
        Instant now = Instant.now();

        List<MemorySeed> seeds = new ArrayList<>();
        seeds.addAll(buildSeedsFromWorldEvents(worldId, owner, size));
        seeds.addAll(buildSeedsFromEventLog(worldId, owner, size));

        int createdCount = 0;
        int deduplicatedCount = 0;
        for (MemorySeed seed : seeds) {
            if (seed == null || seed.summary == null || seed.summary.trim().isEmpty()) {
                continue;
            }
            String summaryHash = hashSummary(seed.summary);
            Optional<MemoryItem> existing = memoryItemRepository.findFirstByWorldIdAndOwnerIdAndSourceEventIdAndSummaryHash(
                    worldId,
                    seed.ownerId,
                    seed.sourceEventId,
                    summaryHash
            );
            if (existing.isPresent()) {
                MemoryItem item = existing.get();
                item.setSummary(seed.summary);
                item.setSalience(Math.max(item.getSalience(), seed.salience));
                item.setDecayRate(seed.decayRate);
                item.setTagsJson(seed.tagsJson);
                item.setExpiresAt(now.plus(Duration.ofDays(7)));
                item.setUpdatedAt(now);
                memoryItemRepository.save(item);
                deduplicatedCount += 1;
                continue;
            }

            MemoryItem item = new MemoryItem(UUID.randomUUID(), worldId, seed.ownerId, seed.sourceEventId, seed.summary);
            item.setSummaryHash(summaryHash);
            item.setSalience(seed.salience);
            item.setDecayRate(seed.decayRate);
            item.setTagsJson(seed.tagsJson);
            item.setExpiresAt(now.plus(Duration.ofDays(7)));
            item.setUpdatedAt(now);
            memoryItemRepository.save(item);
            createdCount += 1;
        }

        List<MemoryItem> rows = listMemories(worldId, owner, size, min);
        return new MemoryCompileResult(worldId, owner, createdCount, deduplicatedCount, now, rows);
    }

    public List<MemoryItem> listMemories(UUID worldId, String ownerId, Integer limit, Double minSalience) {
        String owner = clean(ownerId);
        int size = resolveMemoryLimit(limit);
        double min = normalizeMinSalience(minSalience);
        int fetchSize = Math.max(size * 3, size);
        List<MemoryItem> source = owner == null
                ? memoryItemRepository.findByWorldIdOrderByUpdatedAtDesc(worldId, PageRequest.of(0, fetchSize))
                : memoryItemRepository.findByWorldIdAndOwnerIdOrderByUpdatedAtDesc(worldId, owner, PageRequest.of(0, fetchSize));

        Instant now = Instant.now();
        List<MemoryItem> rows = new ArrayList<>();
        for (MemoryItem item : source) {
            if (item == null) {
                continue;
            }
            decaySalience(item, now);
            if (isExpired(item, now)) {
                continue;
            }
            if (item.getSalience() == null || item.getSalience() < min) {
                continue;
            }
            rows.add(item);
        }
        rows.sort(new Comparator<MemoryItem>() {
            @Override
            public int compare(MemoryItem left, MemoryItem right) {
                int salienceCompare = Double.compare(safeDouble(right.getSalience()), safeDouble(left.getSalience()));
                if (salienceCompare != 0) {
                    return salienceCompare;
                }
                Instant leftUpdated = left.getUpdatedAt() == null ? Instant.EPOCH : left.getUpdatedAt();
                Instant rightUpdated = right.getUpdatedAt() == null ? Instant.EPOCH : right.getUpdatedAt();
                return rightUpdated.compareTo(leftUpdated);
            }
        });
        if (rows.size() > size) {
            return new ArrayList<>(rows.subList(0, size));
        }
        return rows;
    }

    private List<MemorySeed> buildSeedsFromWorldEvents(UUID worldId, String ownerId, int limit) {
        List<WorldEvent> events = worldEventRepository.findByWorldIdOrderByCreatedAtDesc(worldId, PageRequest.of(0, limit));
        List<MemorySeed> seeds = new ArrayList<>();
        for (WorldEvent event : events) {
            if (event == null) {
                continue;
            }
            String owner = ownerId == null ? safeOwner(clean(event.getActorId())) : ownerId;
            String summary = summarizeWorldEvent(event);
            String sourceEventId = event.getEventId() == null ? "world-event:none" : event.getEventId().toString();
            String tags = "{\"source\":\"world_event\",\"event_type\":\"" + safe(event.getEventType()) + "\"}";
            double salience = "WORLD_EVOLUTION".equalsIgnoreCase(safe(event.getEventType())) ? 0.50 : 0.45;
            seeds.add(new MemorySeed(owner, sourceEventId, summary, tags, salience, DEFAULT_MEMORY_DECAY_RATE));
        }
        return seeds;
    }

    @SuppressWarnings("unchecked")
    private List<MemorySeed> buildSeedsFromEventLog(UUID worldId, String ownerId, int limit) {
        if (restTemplate == null || eventServiceUrl == null || eventServiceUrl.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<MemorySeed> seeds = new ArrayList<>();
        try {
            String endpoint = eventServiceUrl + "/api/v2/events?limit=" + limit;
            Map<String, Object> response = restTemplate.getForObject(endpoint, Map.class);
            if (response == null || !(response.get("data") instanceof List)) {
                return seeds;
            }
            for (Object row : (List<?>) response.get("data")) {
                if (!(row instanceof Map)) {
                    continue;
                }
                Map<String, Object> event = new HashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) row).entrySet()) {
                    event.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                String actorId = clean(asText(event.get("actorId")));
                String resolvedOwner = ownerId == null ? safeOwner(actorId) : ownerId;
                String sourceEventId = asText(event.get("eventId"));
                if (sourceEventId == null || sourceEventId.trim().isEmpty()) {
                    sourceEventId = "event-log:none";
                }
                String summary = summarizeEventLog(event, worldId);
                String eventType = safe(asText(event.get("eventType")));
                String tags = "{\"source\":\"event_log\",\"event_type\":\"" + eventType + "\"}";
                double salience = eventType.startsWith("CHAT_") ? 0.55 : 0.40;
                seeds.add(new MemorySeed(resolvedOwner, sourceEventId, summary, tags, salience, 0.04));
            }
        } catch (Exception ignored) {
            return seeds;
        }
        return seeds;
    }

    private String summarizeWorldEvent(WorldEvent event) {
        String payload = safe(event.getPayloadJson());
        String eventType = safe(event.getEventType());
        String actorId = safe(event.getActorId());
        String summary = extractSummaryFromPayload(payload);
        if (summary == null || summary.trim().isEmpty()) {
            summary = eventType + " by " + actorId;
        }
        return summary.trim();
    }

    private String summarizeEventLog(Map<String, Object> row, UUID worldId) {
        String payload = asText(row.get("payloadJson"));
        String eventType = safe(asText(row.get("eventType")));
        String actorId = safe(asText(row.get("actorId")));
        String summary = extractSummaryFromPayload(payload);
        if (summary == null || summary.trim().isEmpty()) {
            summary = eventType + " by " + actorId;
        }
        if (worldId != null && summary.length() < 120) {
            return summary + " [world " + worldId + "]";
        }
        return summary;
    }

    private String extractSummaryFromPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.isObject()) {
                if (node.hasNonNull("summary")) {
                    return node.get("summary").asText();
                }
                if (node.hasNonNull("content")) {
                    return node.get("content").asText();
                }
                if (node.hasNonNull("message")) {
                    return node.get("message").asText();
                }
            }
            return payload.length() > 180 ? payload.substring(0, 180) : payload;
        } catch (Exception ignored) {
            return payload.length() > 180 ? payload.substring(0, 180) : payload;
        }
    }

    private void decaySalience(MemoryItem item, Instant now) {
        if (item.getUpdatedAt() == null || item.getSalience() == null || item.getDecayRate() == null) {
            return;
        }
        long minutes = Math.max(0L, Duration.between(item.getUpdatedAt(), now).toMinutes());
        if (minutes <= 0L) {
            return;
        }
        double days = minutes / (60.0 * 24.0);
        double decayed = safeDouble(item.getSalience()) * Math.exp(-safeDouble(item.getDecayRate()) * days);
        item.setSalience(clamp01(decayed));
        item.setUpdatedAt(now);
        if (safeDouble(item.getSalience()) < MEMORY_MIN_SALIENCE) {
            item.setExpiresAt(now.minusSeconds(1));
        }
        memoryItemRepository.save(item);
    }

    private boolean isExpired(MemoryItem item, Instant now) {
        return item.getExpiresAt() != null && item.getExpiresAt().isBefore(now);
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
                .orElseGet(new java.util.function.Supplier<WorldState>() {
                    @Override
                    public WorldState get() {
                        return worldStateRepository.save(new WorldState(worldId));
                    }
                });
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

    private int resolveMemoryLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 120);
    }

    private int resolveDepth(Integer depth) {
        if (depth == null || depth <= 0) {
            return 2;
        }
        return Math.min(depth, 5);
    }

    private double normalizeMinSalience(Double value) {
        if (value == null) {
            return MEMORY_MIN_SALIENCE;
        }
        return clamp01(value);
    }

    private String hashSummary(String summary) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(summary.toLowerCase(Locale.ROOT).trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            return Integer.toHexString(summary.hashCode());
        }
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeOwner(String value) {
        return value == null ? "director" : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static class MemorySeed {
        private final String ownerId;
        private final String sourceEventId;
        private final String summary;
        private final String tagsJson;
        private final double salience;
        private final double decayRate;

        private MemorySeed(String ownerId,
                           String sourceEventId,
                           String summary,
                           String tagsJson,
                           double salience,
                           double decayRate) {
            this.ownerId = ownerId;
            this.sourceEventId = sourceEventId;
            this.summary = summary;
            this.tagsJson = tagsJson;
            this.salience = salience;
            this.decayRate = decayRate;
        }
    }

    public static class MemoryCompileResult {
        private final UUID worldId;
        private final String ownerId;
        private final int createdCount;
        private final int deduplicatedCount;
        private final Instant compiledAt;
        private final List<MemoryItem> memories;

        public MemoryCompileResult(UUID worldId,
                                   String ownerId,
                                   int createdCount,
                                   int deduplicatedCount,
                                   Instant compiledAt,
                                   List<MemoryItem> memories) {
            this.worldId = worldId;
            this.ownerId = ownerId;
            this.createdCount = createdCount;
            this.deduplicatedCount = deduplicatedCount;
            this.compiledAt = compiledAt;
            this.memories = memories;
        }

        public UUID getWorldId() {
            return worldId;
        }

        public String getOwnerId() {
            return ownerId;
        }

        public int getCreatedCount() {
            return createdCount;
        }

        public int getDeduplicatedCount() {
            return deduplicatedCount;
        }

        public Instant getCompiledAt() {
            return compiledAt;
        }

        public List<MemoryItem> getMemories() {
            return memories;
        }
    }
}
