package com.tarotalk.feed;

import com.tarotalk.feed.domain.Feed;
import com.tarotalk.feed.repo.FeedInteractionRepository;
import com.tarotalk.feed.repo.FeedRepository;
import com.tarotalk.feed.service.FeedEventPublisher;
import com.tarotalk.feed.service.FeedService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class FeedVisibilityTest {
    @Test
    void listVisibleReturnsFeedsForAuthors() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(feedRepository, interactionRepository, eventPublisher, restTemplate, "http://user", "http://rel", "contact", 72, 200);

        UUID viewerId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID feedId = UUID.randomUUID();

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("data", List.of(authorId.toString()));
        when(restTemplate.getForObject(eq("http://user/api/contacts/ids?userId=" + viewerId), eq(Map.class)))
                .thenReturn(apiResponse);

        Feed feed = new Feed(feedId, authorId, "Visible feed");
        when(feedRepository.findByAuthorIdInOrderByCreatedAtDesc(eq(List.of(authorId)), any()))
                .thenReturn(new PageImpl<>(List.of(feed)));

        List<Feed> result = feedService.listVisible(viewerId, null, false, null, null);
        assertEquals(1, result.size());
        assertEquals(feedId, result.get(0).getFeedId());
    }

    @Test
    void listVisibleReturnsEmptyWhenNoAuthors() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(feedRepository, interactionRepository, eventPublisher, restTemplate, "http://user", "http://rel", "contact", 72, 200);
        UUID viewerId = UUID.randomUUID();

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("data", List.of());
        when(restTemplate.getForObject(eq("http://user/api/contacts/ids?userId=" + viewerId), eq(Map.class)))
                .thenReturn(apiResponse);

        List<Feed> result = feedService.listVisible(viewerId, null, false, null, null);
        assertTrue(result.isEmpty());
        verify(feedRepository, never()).findByAuthorIdInOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void listVisibleUsesRelationshipStrategy() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(feedRepository, interactionRepository, eventPublisher, restTemplate, "http://user", "http://rel", "relationship", 72, 200);
        UUID viewerId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Map<String, Object> relation = new HashMap<>();
        relation.put("targetId", authorId.toString());
        Map<String, Object> response = new HashMap<>();
        response.put("data", List.of(relation));
        when(restTemplate.getForObject(eq("http://rel/api/relationships/" + viewerId), eq(Map.class)))
                .thenReturn(response);

        when(feedRepository.findByAuthorIdInOrderByCreatedAtDesc(eq(List.of(authorId)), any()))
                .thenReturn(new PageImpl<>(List.of(new Feed(UUID.randomUUID(), authorId, "From relation"))));

        List<Feed> result = feedService.listVisible(viewerId, null, false, null, null);
        assertEquals(1, result.size());
    }

    @Test
    void listVisibleUsesWorldStrategy() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(
                feedRepository,
                interactionRepository,
                eventPublisher,
                restTemplate,
                "http://user",
                "http://rel",
                "http://world",
                "world",
                72,
                200
        );
        UUID viewerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Map<String, Object> worldRow = new HashMap<>();
        worldRow.put("actorId", actorId.toString());
        Map<String, Object> worldResponse = new HashMap<>();
        worldResponse.put("data", List.of(worldRow));
        when(restTemplate.getForObject(eq("http://world/api/v2/worlds/" + viewerId + "/timeline?limit=30"), eq(Map.class)))
                .thenReturn(worldResponse);

        when(feedRepository.findByAuthorIdInOrderByCreatedAtDesc(eq(List.of(actorId)), any()))
                .thenReturn(new PageImpl<>(List.of(new Feed(UUID.randomUUID(), actorId, "World feed"))));

        List<Feed> result = feedService.listVisible(viewerId, null, false, null, null);
        assertEquals(1, result.size());
        assertEquals(actorId, result.get(0).getAuthorId());
    }

    @Test
    void listVisibleIncludesSelfByDefaultFlag() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(feedRepository, interactionRepository, eventPublisher, restTemplate, "http://user", "http://rel", "contact", 72, 200);
        UUID viewerId = UUID.randomUUID();
        Feed ownFeed = new Feed(UUID.randomUUID(), viewerId, "Own feed");

        when(restTemplate.getForObject(eq("http://user/api/contacts/ids?userId=" + viewerId), eq(Map.class)))
                .thenReturn(new HashMap<>());
        when(feedRepository.findByAuthorIdInOrderByCreatedAtDesc(eq(List.of(viewerId)), any()))
                .thenReturn(new PageImpl<>(List.of(ownFeed)));

        List<Feed> result = feedService.listVisible(viewerId, null, true, null, null);
        assertEquals(1, result.size());
        assertEquals(viewerId, result.get(0).getAuthorId());
    }
}

