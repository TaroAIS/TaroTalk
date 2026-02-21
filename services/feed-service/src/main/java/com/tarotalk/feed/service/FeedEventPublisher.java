package com.tarotalk.feed.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.common.web.TraceContext;
import com.tarotalk.feed.domain.Feed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class FeedEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(FeedEventPublisher.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String userServiceUrl;
    private final String notificationServiceUrl;
    private final String eventServiceUrl;
    private final String relationshipServiceUrl;
    private final String worldServiceUrl;
    private final String fanoutStrategy;
    private final int fanoutMaxRecipients;

    public FeedEventPublisher(RestTemplate restTemplate,
                              ObjectMapper objectMapper,
                              String userServiceUrl,
                              String notificationServiceUrl,
                              String eventServiceUrl) {
        this(
                restTemplate,
                objectMapper,
                userServiceUrl,
                notificationServiceUrl,
                eventServiceUrl,
                "",
                "",
                "hybrid",
                60
        );
    }

    @Autowired
    public FeedEventPublisher(RestTemplate restTemplate,
                              ObjectMapper objectMapper,
                              @Value("${integrations.user-service.base-url:}") String userServiceUrl,
                              @Value("${integrations.notification-service.base-url:}") String notificationServiceUrl,
                              @Value("${integrations.event-service.base-url:}") String eventServiceUrl,
                              @Value("${integrations.relationship-service.base-url:}") String relationshipServiceUrl,
                              @Value("${integrations.world-service.base-url:}") String worldServiceUrl,
                              @Value("${feed.fanout.strategy:hybrid}") String fanoutStrategy,
                              @Value("${feed.fanout.max-recipients:60}") int fanoutMaxRecipients) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userServiceUrl = userServiceUrl;
        this.notificationServiceUrl = notificationServiceUrl;
        this.eventServiceUrl = eventServiceUrl;
        this.relationshipServiceUrl = relationshipServiceUrl;
        this.worldServiceUrl = worldServiceUrl;
        this.fanoutStrategy = fanoutStrategy;
        this.fanoutMaxRecipients = fanoutMaxRecipients;
    }

    public String publishFeedCreated(Feed feed) {
        Set<UUID> recipients = resolveFanoutRecipients(feed.getAuthorId(), feed.getAuthorId());
        String summary = summarize(feed.getContent());
        String content = buildContent(feed.getFeedId(), feed.getAuthorId(), feed.getAuthorId(), summary);
        for (UUID userId : recipients) {
            createNotification(userId, "FEED_CREATED", "new feed", content);
        }
        return appendEvent("FEED_CREATED", feed.getAuthorId(), feed.getFeedId(), summary);
    }

    public String publishFeedLiked(Feed feed, UUID actorId) {
        Set<UUID> recipients = resolveFanoutRecipients(feed.getAuthorId(), actorId);
        recipients.add(feed.getAuthorId());

        String summary = summarize(feed.getContent());
        String content = buildContent(feed.getFeedId(), feed.getAuthorId(), actorId, summary);
        for (UUID userId : recipients) {
            createNotification(userId, "FEED_LIKED", "feed liked", content);
        }
        return appendEvent("FEED_LIKED", actorId, feed.getFeedId(), summary);
    }

    public String publishFeedCommented(Feed feed, UUID actorId, String comment) {
        Set<UUID> recipients = resolveFanoutRecipients(feed.getAuthorId(), actorId);
        recipients.add(feed.getAuthorId());

        String summary = summarize(comment);
        String content = buildContent(feed.getFeedId(), feed.getAuthorId(), actorId, summary);
        for (UUID userId : recipients) {
            createNotification(userId, "FEED_COMMENTED", "new comment", content);
        }
        return appendEvent("FEED_COMMENTED", actorId, feed.getFeedId(), summary);
    }

    public String publishFeedUnliked(Feed feed, UUID actorId) {
        Set<UUID> recipients = new LinkedHashSet<>();
        if (feed.getAuthorId() != null && !feed.getAuthorId().equals(actorId)) {
            recipients.add(feed.getAuthorId());
        }
        String summary = summarize(feed.getContent());
        String content = buildContent(feed.getFeedId(), feed.getAuthorId(), actorId, summary);
        for (UUID userId : recipients) {
            createNotification(userId, "FEED_UNLIKED", "feed unliked", content);
        }
        return appendEvent("FEED_UNLIKED", actorId, feed.getFeedId(), summary);
    }

    private Set<UUID> resolveFanoutRecipients(UUID authorId, UUID actorId) {
        Set<UUID> recipients = new LinkedHashSet<>();
        String strategy = fanoutStrategy == null ? "hybrid" : fanoutStrategy.trim().toLowerCase(Locale.ROOT);

        if ("contact".equals(strategy) || "hybrid".equals(strategy)) {
            recipients.addAll(fetchContactIds(authorId));
        }
        if ("relationship".equals(strategy) || "hybrid".equals(strategy)) {
            recipients.addAll(fetchRelationshipIds(authorId));
        }
        if ("world".equals(strategy) || "hybrid".equals(strategy)) {
            recipients.addAll(fetchWorldContextIds(authorId));
        }

        recipients.remove(authorId);
        recipients.remove(actorId);
        return trimRecipients(recipients);
    }

    private Set<UUID> trimRecipients(Set<UUID> candidates) {
        if (candidates.isEmpty()) {
            return new LinkedHashSet<>();
        }
        int maxCount = fanoutMaxRecipients <= 0 ? 60 : fanoutMaxRecipients;
        Set<UUID> trimmed = new LinkedHashSet<>();
        for (UUID candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            trimmed.add(candidate);
            if (trimmed.size() >= maxCount) {
                break;
            }
        }
        return trimmed;
    }

    private List<UUID> fetchContactIds(UUID authorId) {
        if (userServiceUrl == null || userServiceUrl.trim().isEmpty() || authorId == null) {
            return Collections.emptyList();
        }
        try {
            Map response = restTemplate.getForObject(userServiceUrl + "/api/contacts/ids?userId=" + authorId, Map.class);
            if (response == null || !(response.get("data") instanceof List)) {
                return Collections.emptyList();
            }
            return parseUuidList((List<?>) response.get("data"));
        } catch (Exception ex) {
            log.warn("fetch contact ids failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<UUID> fetchRelationshipIds(UUID authorId) {
        if (relationshipServiceUrl == null || relationshipServiceUrl.trim().isEmpty() || authorId == null) {
            return Collections.emptyList();
        }
        try {
            Map response = restTemplate.getForObject(relationshipServiceUrl + "/api/relationships/" + authorId, Map.class);
            if (response == null || !(response.get("data") instanceof List)) {
                return Collections.emptyList();
            }
            List<UUID> ids = new ArrayList<>();
            for (Object row : (List<?>) response.get("data")) {
                if (!(row instanceof Map)) {
                    continue;
                }
                Object targetId = ((Map<?, ?>) row).get("targetId");
                UUID parsed = parseUuid(targetId);
                if (parsed != null) {
                    ids.add(parsed);
                }
            }
            return ids;
        } catch (Exception ex) {
            log.warn("fetch relationship ids failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<UUID> fetchWorldContextIds(UUID worldId) {
        if (worldServiceUrl == null || worldServiceUrl.trim().isEmpty() || worldId == null) {
            return Collections.emptyList();
        }
        try {
            Map response = restTemplate.getForObject(worldServiceUrl + "/api/v2/worlds/" + worldId + "/timeline?limit=30", Map.class);
            if (response == null || !(response.get("data") instanceof List)) {
                return Collections.emptyList();
            }
            List<UUID> ids = new ArrayList<>();
            for (Object row : (List<?>) response.get("data")) {
                if (!(row instanceof Map)) {
                    continue;
                }
                Object actorId = firstNonNull(((Map<?, ?>) row).get("actorId"), ((Map<?, ?>) row).get("actor_id"));
                UUID parsed = parseUuid(actorId);
                if (parsed != null) {
                    ids.add(parsed);
                }
            }
            return ids;
        } catch (Exception ex) {
            log.warn("fetch world context ids failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private void createNotification(UUID userId, String type, String title, String content) {
        if (notificationServiceUrl == null || notificationServiceUrl.trim().isEmpty() || userId == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId.toString());
        payload.put("type", type);
        payload.put("title", title);
        payload.put("content", content);
        try {
            restTemplate.postForObject(notificationServiceUrl + "/api/notifications", payload, Map.class);
        } catch (Exception ex) {
            log.warn("create notification failed: {}", ex.getMessage());
        }
    }

    private String buildContent(UUID feedId, UUID authorId, UUID actorId, String summary) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("feedId", feedId == null ? null : feedId.toString());
        payload.put("authorId", authorId == null ? null : authorId.toString());
        payload.put("actorId", actorId == null ? null : actorId.toString());
        payload.put("summary", summary);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"feedId\":\"" + feedId + "\",\"authorId\":\"" + authorId + "\",\"actorId\":\"" + actorId + "\",\"summary\":\"" + summary + "\"}";
        }
    }

    private String summarize(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    private String appendEvent(String eventType, UUID actorId, UUID feedId, String summary) {
        if (eventServiceUrl == null || eventServiceUrl.trim().isEmpty()) {
            return null;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("feedId", feedId == null ? null : feedId.toString());
        payload.put("summary", summary);

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", eventType);
        request.put("actorId", actorId == null ? null : actorId.toString());
        request.put("targetId", feedId == null ? null : feedId.toString());
        request.put("entityType", "FEED");
        request.put("entityId", feedId == null ? null : feedId.toString());
        request.put("sourceService", "feed-service");
        request.put("traceId", TraceContext.currentTraceIdOrRandom());
        request.put("idempotencyKey", buildIdempotencyKey(eventType, actorId, feedId));
        request.put("payload", payload);

        try {
            Map response = restTemplate.postForObject(eventServiceUrl + "/internal/events", request, Map.class);
            if (response == null) {
                return null;
            }
            Object data = response.get("data");
            if (data instanceof Map) {
                Object eventId = ((Map<?, ?>) data).get("eventId");
                return eventId == null ? null : String.valueOf(eventId);
            }
            Object eventId = response.get("eventId");
            return eventId == null ? null : String.valueOf(eventId);
        } catch (Exception ex) {
            log.warn("append event log failed: {}", ex.getMessage());
            return null;
        }
    }

    private List<UUID> parseUuidList(List<?> values) {
        List<UUID> ids = new ArrayList<>();
        for (Object value : values) {
            UUID parsed = parseUuid(value);
            if (parsed != null) {
                ids.add(parsed);
            }
        }
        return ids;
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

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String buildIdempotencyKey(String eventType, UUID actorId, UUID feedId) {
        String safeType = eventType == null ? "UNKNOWN" : eventType;
        String safeActor = actorId == null ? "none" : actorId.toString();
        String safeFeed = feedId == null ? "none" : feedId.toString();
        return safeType + ":" + safeFeed + ":" + safeActor;
    }
}
