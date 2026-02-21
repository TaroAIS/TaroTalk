package com.tarotalk.feed.service;

import com.tarotalk.common.exception.ApiException;
import com.tarotalk.feed.api.CommentRequest;
import com.tarotalk.feed.api.CreateFeedRequest;
import com.tarotalk.feed.api.LikeActionRequest;
import com.tarotalk.feed.api.LikeRequest;
import com.tarotalk.feed.domain.Feed;
import com.tarotalk.feed.domain.FeedInteraction;
import com.tarotalk.feed.repo.FeedInteractionRepository;
import com.tarotalk.feed.repo.FeedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedService {
    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
    private static final double DEFAULT_FRESHNESS_WEIGHT = 0.35;
    private static final double DEFAULT_INTERACTION_WEIGHT = 0.25;
    private static final double DEFAULT_RELATIONSHIP_WEIGHT = 0.20;
    private static final double DEFAULT_NARRATIVE_WEIGHT = 0.20;

    private final FeedRepository feedRepository;
    private final FeedInteractionRepository interactionRepository;
    private final FeedEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private final String relationshipServiceUrl;
    private final String worldServiceUrl;
    private final String visibilityStrategy;
    private final long eventTtlHours;
    private final int eventMaxCount;
    private final double freshnessWeight;
    private final double interactionVelocityWeight;
    private final double relationshipAffinityWeight;
    private final double narrativeRelevanceWeight;

    public FeedService(FeedRepository feedRepository,
                       FeedInteractionRepository interactionRepository,
                       FeedEventPublisher eventPublisher,
                       RestTemplate restTemplate,
                       String userServiceUrl,
                       String relationshipServiceUrl,
                       String visibilityStrategy,
                       long eventTtlHours,
                       int eventMaxCount) {
        this(
                feedRepository,
                interactionRepository,
                eventPublisher,
                restTemplate,
                userServiceUrl,
                relationshipServiceUrl,
                "",
                visibilityStrategy,
                eventTtlHours,
                eventMaxCount
        );
    }

    public FeedService(FeedRepository feedRepository,
                       FeedInteractionRepository interactionRepository,
                       FeedEventPublisher eventPublisher,
                       RestTemplate restTemplate,
                       String userServiceUrl,
                       String relationshipServiceUrl,
                       String worldServiceUrl,
                       String visibilityStrategy,
                       long eventTtlHours,
                       int eventMaxCount) {
        this(
                feedRepository,
                interactionRepository,
                eventPublisher,
                restTemplate,
                userServiceUrl,
                relationshipServiceUrl,
                worldServiceUrl,
                visibilityStrategy,
                eventTtlHours,
                eventMaxCount,
                DEFAULT_FRESHNESS_WEIGHT,
                DEFAULT_INTERACTION_WEIGHT,
                DEFAULT_RELATIONSHIP_WEIGHT,
                DEFAULT_NARRATIVE_WEIGHT
        );
    }

    @Autowired
    public FeedService(FeedRepository feedRepository,
                       FeedInteractionRepository interactionRepository,
                       FeedEventPublisher eventPublisher,
                       RestTemplate restTemplate,
                       @Value("${integrations.user-service.base-url:}") String userServiceUrl,
                       @Value("${integrations.relationship-service.base-url:}") String relationshipServiceUrl,
                       @Value("${integrations.world-service.base-url:}") String worldServiceUrl,
                       @Value("${feed.visibility.strategy:contact}") String visibilityStrategy,
                       @Value("${feed.event.ttl-hours:72}") long eventTtlHours,
                       @Value("${feed.event.max-count:200}") int eventMaxCount,
                       @Value("${feed.ranking.weights.freshness:0.35}") double freshnessWeight,
                       @Value("${feed.ranking.weights.interaction-velocity:0.25}") double interactionVelocityWeight,
                       @Value("${feed.ranking.weights.relationship-affinity:0.20}") double relationshipAffinityWeight,
                       @Value("${feed.ranking.weights.narrative-relevance:0.20}") double narrativeRelevanceWeight) {
        this.feedRepository = feedRepository;
        this.interactionRepository = interactionRepository;
        this.eventPublisher = eventPublisher;
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        this.relationshipServiceUrl = relationshipServiceUrl;
        this.worldServiceUrl = worldServiceUrl;
        this.visibilityStrategy = visibilityStrategy;
        this.eventTtlHours = eventTtlHours;
        this.eventMaxCount = eventMaxCount;
        this.freshnessWeight = positiveWeight(freshnessWeight, DEFAULT_FRESHNESS_WEIGHT);
        this.interactionVelocityWeight = positiveWeight(interactionVelocityWeight, DEFAULT_INTERACTION_WEIGHT);
        this.relationshipAffinityWeight = positiveWeight(relationshipAffinityWeight, DEFAULT_RELATIONSHIP_WEIGHT);
        this.narrativeRelevanceWeight = positiveWeight(narrativeRelevanceWeight, DEFAULT_NARRATIVE_WEIGHT);
    }

    public Feed create(CreateFeedRequest request) {
        Feed feed = new Feed(UUID.randomUUID(), request.getAuthorId(), request.getContent());
        feed.setMediaUrls(request.getMediaUrls());
        feed.setLocation(request.getLocation());
        feed.setTopics(request.getTopics());
        feed.setVisibility(request.getVisibility());
        Feed saved = feedRepository.save(feed);
        eventPublisher.publishFeedCreated(saved);
        return saved;
    }

    public List<Feed> list(Integer limit, String cursor) {
        int size = resolveLimit(limit);
        Instant cursorTime = parseCursor(cursor);
        if (cursorTime != null) {
            return feedRepository.findByCreatedAtBeforeOrderByCreatedAtDesc(cursorTime, PageRequest.of(0, size))
                    .getContent();
        }
        return feedRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size, Sort.by("createdAt").descending()))
                .getContent();
    }

    public List<com.tarotalk.feed.api.FeedResponse> buildResponses(List<Feed> feeds, UUID viewerId) {
        if (feeds == null || feeds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> feedIds = feeds.stream().map(Feed::getFeedId).collect(java.util.stream.Collectors.toList());
        List<FeedInteraction> interactions = interactionRepository.findByFeedIdIn(feedIds);
        Map<UUID, long[]> stats = new HashMap<>();
        Map<UUID, List<FeedInteraction>> recentInteractions = new HashMap<>();
        Set<UUID> likedByViewer = new HashSet<>();
        Instant cutoff = Instant.now().minus(Duration.ofHours(eventTtlHours));

        for (FeedInteraction interaction : interactions) {
            long[] counters = stats.computeIfAbsent(interaction.getFeedId(), key -> new long[]{0L, 0L});
            if (interaction.getType() == FeedInteraction.Type.LIKE) {
                counters[0] += 1;
                if (viewerId != null && viewerId.equals(interaction.getUserId())) {
                    likedByViewer.add(interaction.getFeedId());
                }
            }
            if (interaction.getType() == FeedInteraction.Type.COMMENT) {
                counters[1] += 1;
            }
            if (interaction.getCreatedAt() != null && interaction.getCreatedAt().isAfter(cutoff)) {
                recentInteractions.computeIfAbsent(interaction.getFeedId(), key -> new ArrayList<>())
                        .add(interaction);
            }
        }

        List<Feed> ordered = feeds;
        if (viewerId != null) {
            Map<UUID, Double> relationshipWeights = fetchRelationshipWeights(viewerId);
            NarrativeProfile profile = fetchNarrativeProfile(viewerId);
            Map<UUID, Double> scores = new HashMap<>();
            Instant now = Instant.now();

            for (Feed feed : feeds) {
                List<FeedInteraction> recent = recentInteractions.getOrDefault(feed.getFeedId(), Collections.emptyList());
                recent.sort((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
                if (recent.size() > eventMaxCount) {
                    recent = recent.subList(0, eventMaxCount);
                }

                double freshness = scoreFreshness(feed.getCreatedAt(), now);
                double interactionVelocity = scoreInteractionVelocity(recent, now);
                double relationshipAffinity = clamp01(relationshipWeights.getOrDefault(feed.getAuthorId(), 0.0));
                double narrativeRelevance = scoreNarrativeRelevance(feed, profile);
                double weightTotal = weightTotal();

                double totalScore = freshness * (freshnessWeight / weightTotal)
                        + interactionVelocity * (interactionVelocityWeight / weightTotal)
                        + relationshipAffinity * (relationshipAffinityWeight / weightTotal)
                        + narrativeRelevance * (narrativeRelevanceWeight / weightTotal);
                scores.put(feed.getFeedId(), totalScore);
            }

            ordered = new ArrayList<>(feeds);
            ordered.sort((left, right) -> Double.compare(
                    scores.getOrDefault(right.getFeedId(), 0.0),
                    scores.getOrDefault(left.getFeedId(), 0.0)
            ));
        }

        List<com.tarotalk.feed.api.FeedResponse> responses = new ArrayList<>();
        for (Feed feed : ordered) {
            long[] counters = stats.getOrDefault(feed.getFeedId(), new long[]{0L, 0L});
            boolean liked = viewerId != null && likedByViewer.contains(feed.getFeedId());
            responses.add(com.tarotalk.feed.api.FeedResponse.from(feed, counters[0], counters[1], liked));
        }
        return responses;
    }

    public List<Feed> listVisible(UUID viewerId, String visibilityOverride, boolean includeSelf, Integer limit, String cursor) {
        Set<UUID> authorIds = resolveVisibleAuthorIds(viewerId, visibilityOverride, includeSelf);
        if (authorIds.isEmpty()) {
            return Collections.emptyList();
        }
        int size = resolveLimit(limit);
        Instant cursorTime = parseCursor(cursor);
        List<UUID> ids = new ArrayList<>(authorIds);
        if (cursorTime != null) {
            return feedRepository.findByAuthorIdInAndCreatedAtBeforeOrderByCreatedAtDesc(ids, cursorTime, PageRequest.of(0, size))
                    .getContent();
        }
        return feedRepository.findByAuthorIdInOrderByCreatedAtDesc(ids, PageRequest.of(0, size))
                .getContent();
    }

    public FeedInteraction comment(UUID feedId, CommentRequest request) {
        return commentV2(feedId, request).getInteraction();
    }

    public FeedInteraction like(UUID feedId, LikeRequest request) {
        Feed feed = requireFeed(feedId);
        FeedInteraction interaction = new FeedInteraction(UUID.randomUUID(), feedId, request.getUserId(), FeedInteraction.Type.LIKE, null);
        FeedInteraction saved = interactionRepository.save(interaction);
        eventPublisher.publishFeedLiked(feed, request.getUserId());
        syncRelationship(request.getUserId(), feed.getAuthorId(), "LIKE");
        return saved;
    }

    public CommentResult commentV2(UUID feedId, CommentRequest request) {
        Feed feed = requireFeed(feedId);
        FeedInteraction interaction = new FeedInteraction(
                UUID.randomUUID(),
                feedId,
                request.getUserId(),
                FeedInteraction.Type.COMMENT,
                request.getContent()
        );
        FeedInteraction saved = interactionRepository.save(interaction);
        String eventId = eventPublisher.publishFeedCommented(feed, request.getUserId(), request.getContent());
        syncRelationship(request.getUserId(), feed.getAuthorId(), "COMMENT");
        return new CommentResult(saved, eventId);
    }

    public ToggleLikeResult toggleLike(UUID feedId, LikeActionRequest request) {
        Feed feed = requireFeed(feedId);
        java.util.Optional<FeedInteraction> existing = interactionRepository
                .findFirstByFeedIdAndUserIdAndTypeOrderByCreatedAtDesc(feedId, request.getUserId(), FeedInteraction.Type.LIKE);

        if (request.getAction() == LikeActionRequest.Action.LIKE) {
            if (existing.isPresent()) {
                return new ToggleLikeResult(existing.get(), true, null);
            }
            FeedInteraction interaction = new FeedInteraction(
                    UUID.randomUUID(),
                    feedId,
                    request.getUserId(),
                    FeedInteraction.Type.LIKE,
                    null
            );
            FeedInteraction saved = interactionRepository.save(interaction);
            String eventId = eventPublisher.publishFeedLiked(feed, request.getUserId());
            syncRelationship(request.getUserId(), feed.getAuthorId(), "LIKE");
            return new ToggleLikeResult(saved, true, eventId);
        }

        if (existing.isPresent()) {
            interactionRepository.delete(existing.get());
            String eventId = eventPublisher.publishFeedUnliked(feed, request.getUserId());
            syncRelationship(request.getUserId(), feed.getAuthorId(), "UNLIKE");
            return new ToggleLikeResult(null, false, eventId);
        }
        return new ToggleLikeResult(null, false, null);
    }

    private Set<UUID> resolveVisibleAuthorIds(UUID viewerId, String visibilityOverride, boolean includeSelf) {
        String strategy = visibilityOverride == null || visibilityOverride.trim().isEmpty()
                ? visibilityStrategy
                : visibilityOverride.trim().toLowerCase(Locale.ROOT);
        Set<UUID> ids = new HashSet<>();
        if (includeSelf && viewerId != null) {
            ids.add(viewerId);
        }
        if ("contact".equals(strategy) || "hybrid".equals(strategy)) {
            ids.addAll(fetchContactAuthorIds(viewerId));
        }
        if ("relationship".equals(strategy) || "hybrid".equals(strategy)) {
            ids.addAll(fetchRelationshipAuthorIds(viewerId));
        }
        if ("world".equals(strategy) || "hybrid".equals(strategy)) {
            ids.addAll(fetchWorldContextAuthorIds(viewerId));
        }
        return ids;
    }

    private List<UUID> fetchContactAuthorIds(UUID viewerId) {
        if (userServiceUrl == null || userServiceUrl.trim().isEmpty() || viewerId == null) {
            return Collections.emptyList();
        }
        try {
            Map response = restTemplate.getForObject(userServiceUrl + "/api/contacts/ids?userId=" + viewerId, Map.class);
            if (response == null) {
                return Collections.emptyList();
            }
            return parseUuidList(response.get("data"));
        } catch (Exception ex) {
            log.warn("fetch visible authors failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<UUID> fetchRelationshipAuthorIds(UUID viewerId) {
        List<Map<String, Object>> rows = fetchRelationshipRows(viewerId);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            UUID targetId = parseUuid(row.get("targetId"));
            if (targetId != null) {
                ids.add(targetId);
            }
        }
        return ids;
    }

    private Map<UUID, Double> fetchRelationshipWeights(UUID viewerId) {
        List<Map<String, Object>> rows = fetchRelationshipRows(viewerId);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, Double> weights = new HashMap<>();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            UUID targetId = parseUuid(row.get("targetId"));
            if (targetId == null) {
                continue;
            }
            double intimacy = asDouble(row.get("intimacyScore"));
            double interactions = asDouble(row.get("interactionCount"));
            double commercial = asDouble(row.get("commercialScore"));
            double affinity = clamp01(intimacy * 0.7 + Math.min(interactions / 50.0, 1.0) * 0.2 + commercial * 0.1);
            weights.put(targetId, affinity);
        }
        return weights;
    }

    private List<Map<String, Object>> fetchRelationshipRows(UUID userId) {
        if (relationshipServiceUrl == null || relationshipServiceUrl.trim().isEmpty() || userId == null) {
            return Collections.emptyList();
        }
        try {
            Map response = restTemplate.getForObject(relationshipServiceUrl + "/api/relationships/" + userId, Map.class);
            if (response == null) {
                return Collections.emptyList();
            }
            Object data = response.get("data");
            if (!(data instanceof List)) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Object item : (List<?>) data) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> row = new HashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                    row.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception ex) {
            log.warn("fetch relationship rows failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private void syncRelationship(UUID actorId, UUID targetId, String signal) {
        if (actorId == null || targetId == null || actorId.equals(targetId)) {
            return;
        }
        if (relationshipServiceUrl == null || relationshipServiceUrl.trim().isEmpty()) {
            return;
        }

        Map<String, Object> current = findRelationship(actorId, targetId);
        double currentIntimacy = asDouble(current.get("intimacyScore"));
        long currentCount = asLong(current.get("interactionCount"));
        double commercial = asDouble(current.get("commercialScore"));
        String type = normalizeRelationshipType(String.valueOf(current.get("type")));

        double delta;
        if ("COMMENT".equals(signal)) {
            delta = 0.08;
        } else if ("LIKE".equals(signal)) {
            delta = 0.04;
        } else if ("UNLIKE".equals(signal)) {
            delta = -0.03;
        } else {
            delta = 0.0;
        }

        double nextIntimacy = clamp01(currentIntimacy + delta);
        long nextCount = "UNLIKE".equals(signal) ? currentCount : (currentCount + 1L);

        Map<String, Object> request = new HashMap<>();
        request.put("targetId", targetId.toString());
        request.put("type", type);
        request.put("intimacyScore", nextIntimacy);
        request.put("interactionCount", nextCount);
        request.put("commercialScore", commercial);

        try {
            restTemplate.postForObject(relationshipServiceUrl + "/api/relationships/" + actorId, request, Map.class);
        } catch (Exception ex) {
            log.warn("sync relationship failed: {}", ex.getMessage());
        }
    }

    private Map<String, Object> findRelationship(UUID userId, UUID targetId) {
        List<Map<String, Object>> rows = fetchRelationshipRows(userId);
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            UUID rowTarget = parseUuid(row.get("targetId"));
            if (targetId.equals(rowTarget)) {
                return row;
            }
        }
        return Collections.emptyMap();
    }

    private String normalizeRelationshipType(String rawType) {
        if (rawType == null) {
            return "friend";
        }
        String normalized = rawType.trim().toLowerCase(Locale.ROOT);
        if ("friend".equals(normalized)
                || "mentor".equals(normalized)
                || "rival".equals(normalized)
                || "advertiser".equals(normalized)
                || "self-agent".equals(normalized)) {
            return normalized;
        }
        return "friend";
    }

    private NarrativeProfile fetchNarrativeProfile(UUID viewerId) {
        NarrativeProfile profile = new NarrativeProfile();
        if (worldServiceUrl == null || worldServiceUrl.trim().isEmpty() || viewerId == null) {
            return profile;
        }
        try {
            Map response = restTemplate.getForObject(worldServiceUrl + "/api/v2/worlds/" + viewerId + "/timeline?limit=30", Map.class);
            if (response == null || !(response.get("data") instanceof List)) {
                return profile;
            }
            for (Object item : (List<?>) response.get("data")) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> row = (Map<?, ?>) item;
                UUID actorId = parseUuid(firstNonNull(row.get("actorId"), row.get("actor_id")));
                if (actorId != null) {
                    profile.actorIds.add(actorId);
                }
                profile.keywords.addAll(extractKeywords(String.valueOf(firstNonNull(row.get("eventType"), row.get("event_type")))));
                profile.keywords.addAll(extractKeywords(String.valueOf(firstNonNull(row.get("payloadJson"), row.get("payload_json")))));
            }
        } catch (Exception ex) {
            log.warn("fetch narrative profile failed: {}", ex.getMessage());
        }
        return profile;
    }

    private List<UUID> fetchWorldContextAuthorIds(UUID worldId) {
        if (worldServiceUrl == null || worldServiceUrl.trim().isEmpty() || worldId == null) {
            return Collections.emptyList();
        }
        try {
            Map response = restTemplate.getForObject(worldServiceUrl + "/api/v2/worlds/" + worldId + "/timeline?limit=30", Map.class);
            if (response == null || !(response.get("data") instanceof List)) {
                return Collections.emptyList();
            }
            List<UUID> ids = new ArrayList<>();
            for (Object item : (List<?>) response.get("data")) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> row = (Map<?, ?>) item;
                UUID actorId = parseUuid(firstNonNull(row.get("actorId"), row.get("actor_id")));
                if (actorId != null) {
                    ids.add(actorId);
                }
            }
            return ids;
        } catch (Exception ex) {
            log.warn("fetch world context authors failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private double scoreFreshness(Instant createdAt, Instant now) {
        if (createdAt == null || now == null || createdAt.isAfter(now)) {
            return 0.0;
        }
        double ageHours = Duration.between(createdAt, now).toMinutes() / 60.0;
        return Math.exp(-ageHours / 24.0);
    }

    private double scoreInteractionVelocity(List<FeedInteraction> recent, Instant now) {
        if (recent == null || recent.isEmpty() || now == null) {
            return 0.0;
        }
        long likes = recent.stream().filter(item -> item.getType() == FeedInteraction.Type.LIKE).count();
        long comments = recent.stream().filter(item -> item.getType() == FeedInteraction.Type.COMMENT).count();
        Instant oldest = recent.get(recent.size() - 1).getCreatedAt();
        double hours = oldest == null ? 1.0 : Math.max(1.0, Duration.between(oldest, now).toMinutes() / 60.0);
        double weighted = likes + comments * 2.0;
        return clamp01((weighted / hours) / 3.0);
    }

    private double scoreNarrativeRelevance(Feed feed, NarrativeProfile profile) {
        if (feed == null || profile == null) {
            return 0.0;
        }
        double score = 0.0;
        if (feed.getAuthorId() != null && profile.actorIds.contains(feed.getAuthorId())) {
            score += 0.6;
        }

        Set<String> feedKeywords = new HashSet<>();
        feedKeywords.addAll(extractKeywords(feed.getContent()));
        feedKeywords.addAll(extractKeywords(feed.getTopics()));
        feedKeywords.addAll(extractKeywords(feed.getLocation()));
        if (!feedKeywords.isEmpty() && !profile.keywords.isEmpty()) {
            int overlap = 0;
            for (String token : feedKeywords) {
                if (profile.keywords.contains(token)) {
                    overlap += 1;
                }
            }
            score += 0.4 * Math.min(1.0, overlap / 3.0);
        }
        return clamp01(score);
    }

    private Set<String> extractKeywords(String text) {
        if (text == null) {
            return Collections.emptySet();
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("[^\\p{L}\\p{Nd}]+");
        Set<String> keywords = new HashSet<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String token = part.trim();
            if (token.length() < 2) {
                continue;
            }
            keywords.add(token);
        }
        return keywords;
    }

    private List<UUID> parseUuidList(Object data) {
        if (!(data instanceof List)) {
            return Collections.emptyList();
        }
        List<UUID> ids = new ArrayList<>();
        for (Object item : (List<?>) data) {
            UUID id = parseUuid(item);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private double asDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return 0L;
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

    private double positiveWeight(double configured, double fallback) {
        if (configured <= 0.0) {
            return fallback;
        }
        return configured;
    }

    private double weightTotal() {
        double total = freshnessWeight + interactionVelocityWeight + relationshipAffinityWeight + narrativeRelevanceWeight;
        return total > 0.0 ? total : 1.0;
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private Instant parseCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(cursor);
        } catch (Exception ex) {
            throw new ApiException("VALIDATION_ERROR", "invalid cursor");
        }
    }

    private Feed requireFeed(UUID feedId) {
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "feed not found"));
    }

    private static class NarrativeProfile {
        private final Set<UUID> actorIds = new HashSet<>();
        private final Set<String> keywords = new HashSet<>();
    }

    public static class ToggleLikeResult {
        private final FeedInteraction interaction;
        private final boolean liked;
        private final String eventId;

        public ToggleLikeResult(FeedInteraction interaction, boolean liked, String eventId) {
            this.interaction = interaction;
            this.liked = liked;
            this.eventId = eventId;
        }

        public FeedInteraction getInteraction() {
            return interaction;
        }

        public boolean isLiked() {
            return liked;
        }

        public String getEventId() {
            return eventId;
        }
    }

    public static class CommentResult {
        private final FeedInteraction interaction;
        private final String eventId;

        public CommentResult(FeedInteraction interaction, String eventId) {
            this.interaction = interaction;
            this.eventId = eventId;
        }

        public FeedInteraction getInteraction() {
            return interaction;
        }

        public String getEventId() {
            return eventId;
        }
    }
}
