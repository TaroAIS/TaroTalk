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

import java.util.List;
import java.util.UUID;

@Service
public class FeedService {
    private final FeedRepository feedRepository;
    private final FeedInteractionRepository interactionRepository;
    private final FeedEventPublisher eventPublisher;

    public FeedService(FeedRepository feedRepository,
                       FeedInteractionRepository interactionRepository,
                       FeedEventPublisher eventPublisher) {
        this.feedRepository = feedRepository;
        this.interactionRepository = interactionRepository;
        this.eventPublisher = eventPublisher;
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
}
