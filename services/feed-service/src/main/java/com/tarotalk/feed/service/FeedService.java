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

@Service
public class FeedService {
    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
    private final FeedRepository feedRepository;
    private final FeedInteractionRepository interactionRepository;
    private final FeedEventPublisher eventPublisher;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public FeedService(FeedRepository feedRepository,
                       FeedInteractionRepository interactionRepository,
                       FeedEventPublisher eventPublisher,
                       RestTemplate restTemplate,
                       @Value("${integrations.user-service.base-url:}") String userServiceUrl) {
        this.feedRepository = feedRepository;
        this.interactionRepository = interactionRepository;
        this.eventPublisher = eventPublisher;
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
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

    public List<Feed> listVisible(UUID viewerId) {
        List<UUID> authorIds = fetchVisibleAuthorIds(viewerId);
        if (authorIds.isEmpty()) {
            return Collections.emptyList();
        }
        return feedRepository.findTop20ByAuthorIdInOrderByCreatedAtDesc(authorIds);
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

    private List<UUID> fetchVisibleAuthorIds(UUID viewerId) {
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
}
