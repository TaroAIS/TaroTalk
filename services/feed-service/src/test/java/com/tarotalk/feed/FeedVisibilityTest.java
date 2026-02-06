package com.tarotalk.feed;

import com.tarotalk.feed.domain.Feed;
import com.tarotalk.feed.repo.FeedInteractionRepository;
import com.tarotalk.feed.repo.FeedRepository;
import com.tarotalk.feed.service.FeedEventPublisher;
import com.tarotalk.feed.service.FeedService;
import org.junit.jupiter.api.Test;
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

        FeedService feedService = new FeedService(feedRepository, interactionRepository, eventPublisher, restTemplate, "http://user");

        UUID viewerId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID feedId = UUID.randomUUID();

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("data", List.of(authorId.toString()));
        when(restTemplate.getForObject(eq("http://user/api/contacts/owners?contactUserId=" + viewerId), eq(Map.class)))
                .thenReturn(apiResponse);

        Feed feed = new Feed(feedId, authorId, "Visible feed");
        when(feedRepository.findTop20ByAuthorIdInOrderByCreatedAtDesc(eq(List.of(authorId))))
                .thenReturn(List.of(feed));

        List<Feed> result = feedService.listVisible(viewerId);
        assertEquals(1, result.size());
        assertEquals(feedId, result.get(0).getFeedId());
    }

    @Test
    void listVisibleReturnsEmptyWhenNoAuthors() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(feedRepository, interactionRepository, eventPublisher, restTemplate, "http://user");
        UUID viewerId = UUID.randomUUID();

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("data", List.of());
        when(restTemplate.getForObject(eq("http://user/api/contacts/owners?contactUserId=" + viewerId), eq(Map.class)))
                .thenReturn(apiResponse);

        List<Feed> result = feedService.listVisible(viewerId);
        assertTrue(result.isEmpty());
        verify(feedRepository, never()).findTop20ByAuthorIdInOrderByCreatedAtDesc(any());
    }
}
