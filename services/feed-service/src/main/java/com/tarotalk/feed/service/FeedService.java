package com.tarotalk.feed.service;

import com.tarotalk.feed.api.CommentRequest;
import com.tarotalk.feed.api.CreateFeedRequest;
import com.tarotalk.feed.api.LikeRequest;
import com.tarotalk.feed.domain.Feed;
import com.tarotalk.feed.domain.FeedInteraction;
import com.tarotalk.feed.repo.FeedInteractionRepository;
import com.tarotalk.feed.repo.FeedRepository;
import com.tarotalk.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Service
public class FeedService {
    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
    private final FeedRepository feedRepository;
    private final FeedInteractionRepository interactionRepository;
    private final FeedEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private final String relationshipServiceUrl;
    private final String visibilityStrategy;

    public FeedService(FeedRepository feedRepository,
                       FeedInteractionRepository interactionRepository,
                       FeedEventPublisher eventPublisher,
                       RestTemplate restTemplate,
                       @Value("${integrations.user-service.base-url:}") String userServiceUrl,
                       @Value("${integrations.relationship-service.base-url:}") String relationshipServiceUrl,
                       @Value("${feed.visibility.strategy:contact}") String visibilityStrategy) {
        this.feedRepository = feedRepository;
        this.interactionRepository = interactionRepository;
        this.eventPublisher = eventPublisher;
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        this.relationshipServiceUrl = relationshipServiceUrl;
        this.visibilityStrategy = visibilityStrategy;
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

    public List<Feed> list() {
        return feedRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public List<com.tarotalk.feed.api.FeedResponse> buildResponses(List<Feed> feeds, UUID viewerId) {
        if (feeds == null || feeds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<UUID> feedIds = feeds.stream().map(Feed::getFeedId).collect(java.util.stream.Collectors.toList());
        java.util.List<com.tarotalk.feed.domain.FeedInteraction> interactions = interactionRepository.findByFeedIdIn(feedIds);
        java.util.Map<UUID, long[]> stats = new java.util.HashMap<>();
        java.util.Set<UUID> likedByViewer = new java.util.HashSet<>();
        for (com.tarotalk.feed.domain.FeedInteraction interaction : interactions) {
            long[] counters = stats.computeIfAbsent(interaction.getFeedId(), key -> new long[]{0L, 0L});
            if (interaction.getType() == com.tarotalk.feed.domain.FeedInteraction.Type.LIKE) {
                counters[0] += 1;
                if (viewerId != null && viewerId.equals(interaction.getUserId())) {
                    likedByViewer.add(interaction.getFeedId());
                }
            }
            if (interaction.getType() == com.tarotalk.feed.domain.FeedInteraction.Type.COMMENT) {
                counters[1] += 1;
            }
        }
        java.util.List<Feed> ordered = feeds;
        if (viewerId != null) {
            java.util.Map<UUID, Double> relationshipWeights = fetchRelationshipWeights(viewerId);
            java.util.Map<UUID, Double> scores = new java.util.HashMap<>();
            java.time.Instant now = java.time.Instant.now();
            for (Feed feed : feeds) {
                long[] counters = stats.getOrDefault(feed.getFeedId(), new long[]{0L, 0L});
                double likeScore = counters[0] * 0.3;
                double commentScore = counters[1] * 0.6;
                double hours = java.time.Duration.between(feed.getCreatedAt(), now).toMinutes() / 60.0;
                double timeScore = Math.exp(-hours / 24.0);
                double relationScore = relationshipWeights.getOrDefault(feed.getAuthorId(), 0.0);
                scores.put(feed.getFeedId(), timeScore + likeScore + commentScore + relationScore);
            }
            ordered = new java.util.ArrayList<>(feeds);
            ordered.sort((a, b) -> Double.compare(
                    scores.getOrDefault(b.getFeedId(), 0.0),
                    scores.getOrDefault(a.getFeedId(), 0.0)
            ));
        }
        java.util.List<com.tarotalk.feed.api.FeedResponse> responses = new java.util.ArrayList<>();
        for (Feed feed : ordered) {
            long[] counters = stats.getOrDefault(feed.getFeedId(), new long[]{0L, 0L});
            boolean liked = viewerId != null && likedByViewer.contains(feed.getFeedId());
            responses.add(com.tarotalk.feed.api.FeedResponse.from(feed, counters[0], counters[1], liked));
        }
        return responses;
    }

    public List<Feed> listVisible(UUID viewerId, String visibilityOverride) {
        Set<UUID> authorIds = resolveVisibleAuthorIds(viewerId, visibilityOverride);
        if (authorIds.isEmpty()) {
            return Collections.emptyList();
        }
        return feedRepository.findTop20ByAuthorIdInOrderByCreatedAtDesc(new ArrayList<>(authorIds));
    }

    public FeedInteraction comment(UUID feedId, CommentRequest request) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "feed not found"));
        FeedInteraction interaction = new FeedInteraction(UUID.randomUUID(), feedId, request.getUserId(), FeedInteraction.Type.COMMENT, request.getContent());
        FeedInteraction saved = interactionRepository.save(interaction);
        eventPublisher.publishFeedCommented(feed, request.getUserId(), request.getContent());
        return saved;
    }

    public FeedInteraction like(UUID feedId, LikeRequest request) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "feed not found"));
        FeedInteraction interaction = new FeedInteraction(UUID.randomUUID(), feedId, request.getUserId(), FeedInteraction.Type.LIKE, null);
        FeedInteraction saved = interactionRepository.save(interaction);
        eventPublisher.publishFeedLiked(feed, request.getUserId());
        return saved;
    }

    private Set<UUID> resolveVisibleAuthorIds(UUID viewerId, String visibilityOverride) {
        String strategy = visibilityOverride == null || visibilityOverride.trim().isEmpty()
                ? visibilityStrategy
                : visibilityOverride.trim().toLowerCase();
        Set<UUID> ids = new HashSet<>();
        if ("contact".equals(strategy) || "hybrid".equals(strategy)) {
            ids.addAll(fetchContactAuthorIds(viewerId));
        }
        if ("relationship".equals(strategy) || "hybrid".equals(strategy)) {
            ids.addAll(fetchRelationshipAuthorIds(viewerId));
        }
        return ids;
    }

    private List<UUID> fetchContactAuthorIds(UUID viewerId) {
        if (userServiceUrl == null || userServiceUrl.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Map response = restTemplate.getForObject(userServiceUrl + "/api/contacts/owners?contactUserId=" + viewerId, Map.class);
            if (response == null) {
                return Collections.emptyList();
            }
            Object data = response.get("data");
            if (!(data instanceof List)) {
                return Collections.emptyList();
            }
            List<UUID> ids = new ArrayList<>();
            for (Object item : (List<?>) data) {
                try {
                    ids.add(UUID.fromString(String.valueOf(item)));
                } catch (IllegalArgumentException ex) {
                    log.warn("invalid author id: {}", item);
                }
            }
            return ids;
        } catch (Exception ex) {
            log.warn("fetch visible authors failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<UUID> fetchRelationshipAuthorIds(UUID viewerId) {
        if (relationshipServiceUrl == null || relationshipServiceUrl.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Map response = restTemplate.getForObject(relationshipServiceUrl + "/api/relationships/" + viewerId, Map.class);
            if (response == null) {
                return Collections.emptyList();
            }
            Object data = response.get("data");
            if (!(data instanceof List)) {
                return Collections.emptyList();
            }
            List<UUID> ids = new ArrayList<>();
            for (Object item : (List<?>) data) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Object targetId = ((Map<?, ?>) item).get("targetId");
                if (targetId == null) {
                    continue;
                }
                try {
                    ids.add(UUID.fromString(String.valueOf(targetId)));
                } catch (IllegalArgumentException ex) {
                    log.warn("invalid relationship target id: {}", targetId);
                }
            }
            return ids;
        } catch (Exception ex) {
            log.warn("fetch relationship authors failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<UUID, Double> fetchRelationshipWeights(UUID viewerId) {
        if (relationshipServiceUrl == null || relationshipServiceUrl.trim().isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        try {
            Map response = restTemplate.getForObject(relationshipServiceUrl + "/api/relationships/" + viewerId, Map.class);
            if (response == null) {
                return java.util.Collections.emptyMap();
            }
            Object data = response.get("data");
            if (!(data instanceof List)) {
                return java.util.Collections.emptyMap();
            }
            Map<UUID, Double> weights = new java.util.HashMap<>();
            for (Object item : (List<?>) data) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> row = (Map<?, ?>) item;
                Object targetId = row.get("targetId");
                Object intimacyScore = row.get("intimacyScore");
                if (targetId == null) {
                    continue;
                }
                try {
                    UUID id = UUID.fromString(String.valueOf(targetId));
                    double score = intimacyScore instanceof Number ? ((Number) intimacyScore).doubleValue() : 0.0;
                    weights.put(id, score);
                } catch (IllegalArgumentException ex) {
                    log.warn("invalid relationship target id: {}", targetId);
                }
            }
            return weights;
        } catch (Exception ex) {
            log.warn("fetch relationship weights failed: {}", ex.getMessage());
            return java.util.Collections.emptyMap();
        }
    }
}
