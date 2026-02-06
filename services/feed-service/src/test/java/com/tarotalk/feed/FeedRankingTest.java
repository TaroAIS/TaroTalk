package com.tarotalk.feed;

import com.tarotalk.feed.api.FeedResponse;
import com.tarotalk.feed.domain.Feed;
import com.tarotalk.feed.repo.FeedInteractionRepository;
import com.tarotalk.feed.repo.FeedRepository;
import com.tarotalk.feed.service.FeedEventPublisher;
import com.tarotalk.feed.service.FeedService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedRankingTest {
    @Test
    void rankingUsesRelationshipWeight() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(feedRepository, interactionRepository, eventPublisher, restTemplate, "", "http://rel", "contact");

        UUID viewerId = UUID.randomUUID();
        UUID authorA = UUID.randomUUID();
        UUID authorB = UUID.randomUUID();

        Feed feedA = new Feed(UUID.randomUUID(), authorA, "A");
        Feed feedB = new Feed(UUID.randomUUID(), authorB, "B");
        Instant same = Instant.now().minusSeconds(60);
        feedA.setCreatedAt(same);
        feedB.setCreatedAt(same);

        when(interactionRepository.findByFeedIdIn(anyList())).thenReturn(List.of());

        Map<String, Object> relation = new HashMap<>();
        relation.put("targetId", authorB.toString());
        relation.put("intimacyScore", 1.0);
        Map<String, Object> response = new HashMap<>();
        response.put("data", List.of(relation));
        when(restTemplate.getForObject(eq("http://rel/api/relationships/" + viewerId), eq(Map.class)))
                .thenReturn(response);

        List<FeedResponse> responses = feedService.buildResponses(List.of(feedA, feedB), viewerId);
        assertEquals(authorB, responses.get(0).getAuthorId());
    }
}
