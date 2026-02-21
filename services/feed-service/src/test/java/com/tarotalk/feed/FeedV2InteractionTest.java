package com.tarotalk.feed;

import com.tarotalk.feed.api.CommentRequest;
import com.tarotalk.feed.api.LikeActionRequest;
import com.tarotalk.feed.domain.Feed;
import com.tarotalk.feed.domain.FeedInteraction;
import com.tarotalk.feed.repo.FeedInteractionRepository;
import com.tarotalk.feed.repo.FeedRepository;
import com.tarotalk.feed.service.FeedEventPublisher;
import com.tarotalk.feed.service.FeedService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class FeedV2InteractionTest {
    @Test
    void likeToggleIsIdempotentWhenAlreadyLiked() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(
                feedRepository,
                interactionRepository,
                eventPublisher,
                restTemplate,
                "",
                "http://rel",
                "contact",
                72,
                200
        );

        UUID feedId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Feed feed = new Feed(feedId, authorId, "hello");
        FeedInteraction existing = new FeedInteraction(UUID.randomUUID(), feedId, userId, FeedInteraction.Type.LIKE, null);

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(interactionRepository.findFirstByFeedIdAndUserIdAndTypeOrderByCreatedAtDesc(feedId, userId, FeedInteraction.Type.LIKE))
                .thenReturn(Optional.of(existing));

        LikeActionRequest request = new LikeActionRequest();
        request.setUserId(userId);
        request.setAction(LikeActionRequest.Action.LIKE);
        FeedService.ToggleLikeResult result = feedService.toggleLike(feedId, request);

        assertTrue(result.isLiked());
        assertNull(result.getEventId());
        verify(eventPublisher, never()).publishFeedLiked(any(Feed.class), any(UUID.class));
        verify(interactionRepository, never()).save(any(FeedInteraction.class));
    }

    @Test
    void unlikeRemovesLikeAndPublishesEvent() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(
                feedRepository,
                interactionRepository,
                eventPublisher,
                restTemplate,
                "",
                "http://rel",
                "contact",
                72,
                200
        );

        UUID feedId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Feed feed = new Feed(feedId, authorId, "hello");
        FeedInteraction existing = new FeedInteraction(UUID.randomUUID(), feedId, userId, FeedInteraction.Type.LIKE, null);

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(interactionRepository.findFirstByFeedIdAndUserIdAndTypeOrderByCreatedAtDesc(feedId, userId, FeedInteraction.Type.LIKE))
                .thenReturn(Optional.of(existing));
        when(eventPublisher.publishFeedUnliked(feed, userId)).thenReturn("event-1");
        when(restTemplate.getForObject(eq("http://rel/api/relationships/" + userId), eq(java.util.Map.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put("data", Collections.emptyList());
                }});

        LikeActionRequest request = new LikeActionRequest();
        request.setUserId(userId);
        request.setAction(LikeActionRequest.Action.UNLIKE);
        FeedService.ToggleLikeResult result = feedService.toggleLike(feedId, request);

        assertFalse(result.isLiked());
        verify(interactionRepository, times(1)).delete(existing);
        verify(eventPublisher, times(1)).publishFeedUnliked(feed, userId);
    }

    @Test
    void commentTriggersRelationshipSync() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(
                feedRepository,
                interactionRepository,
                eventPublisher,
                restTemplate,
                "",
                "http://rel",
                "contact",
                72,
                200
        );

        UUID feedId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Feed feed = new Feed(feedId, authorId, "hello");

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(feed));
        when(interactionRepository.save(any(FeedInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventPublisher.publishFeedCommented(eq(feed), eq(userId), any(String.class))).thenReturn("event-2");
        when(restTemplate.getForObject(eq("http://rel/api/relationships/" + userId), eq(java.util.Map.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put("data", Collections.emptyList());
                }});

        CommentRequest request = new CommentRequest();
        request.setUserId(userId);
        request.setContent("nice");
        feedService.commentV2(feedId, request);

        verify(restTemplate, times(1))
                .postForObject(eq("http://rel/api/relationships/" + userId), any(java.util.Map.class), eq(java.util.Map.class));
    }
}
