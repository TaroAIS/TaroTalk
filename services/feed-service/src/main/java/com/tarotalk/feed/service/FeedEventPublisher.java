package com.tarotalk.feed.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.feed.domain.Feed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class FeedEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(FeedEventPublisher.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String userServiceUrl;
    private final String notificationServiceUrl;

    public FeedEventPublisher(RestTemplate restTemplate,
                              ObjectMapper objectMapper,
                              @Value("${integrations.user-service.base-url:}") String userServiceUrl,
                              @Value("${integrations.notification-service.base-url:}") String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userServiceUrl = userServiceUrl;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    public void publishFeedCreated(Feed feed) {
        List<UUID> recipients = fetchContactIds(feed.getAuthorId());
        if (recipients.isEmpty()) {
            return;
        }
        String summary = summarize(feed.getContent());
        String content = buildContent(feed.getFeedId(), feed.getAuthorId(), feed.getAuthorId(), summary);
        for (UUID userId : recipients) {
            createNotification(userId, "FEED_CREATED", "新动态", content);
        }
    }

    public void publishFeedLiked(Feed feed, UUID actorId) {
        String summary = summarize(feed.getContent());
        String content = buildContent(feed.getFeedId(), feed.getAuthorId(), actorId, summary);
        createNotification(feed.getAuthorId(), "FEED_LIKED", "收到点赞", content);
    }

    public void publishFeedCommented(Feed feed, UUID actorId, String comment) {
        String summary = summarize(comment);
        String content = buildContent(feed.getFeedId(), feed.getAuthorId(), actorId, summary);
        createNotification(feed.getAuthorId(), "FEED_COMMENTED", "收到评论", content);
    }

    private List<UUID> fetchContactIds(UUID authorId) {
        if (userServiceUrl == null || userServiceUrl.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Map response = restTemplate.getForObject(userServiceUrl + "/api/contacts/ids?userId=" + authorId, Map.class);
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
                    log.warn("invalid contact id: {}", item);
                }
            }
            return ids;
        } catch (Exception ex) {
            log.warn("fetch contact ids failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private void createNotification(UUID userId, String type, String title, String content) {
        if (notificationServiceUrl == null || notificationServiceUrl.trim().isEmpty()) {
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
        payload.put("feedId", feedId.toString());
        payload.put("authorId", authorId.toString());
        payload.put("actorId", actorId.toString());
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
}
