package com.tarotalk.feed;

import com.tarotalk.feed.api.FeedResponse;
import com.tarotalk.feed.domain.Feed;
import com.tarotalk.feed.domain.FeedInteraction;
import com.tarotalk.feed.repo.FeedInteractionRepository;
import com.tarotalk.feed.repo.FeedRepository;
import com.tarotalk.feed.service.FeedEventPublisher;
import com.tarotalk.feed.service.FeedService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedStatsTest {
    @Test
    void buildResponsesIncludesStatsAndLikedFlag() {
        FeedRepository feedRepository = mock(FeedRepository.class);
        FeedInteractionRepository interactionRepository = mock(FeedInteractionRepository.class);
        FeedEventPublisher eventPublisher = mock(FeedEventPublisher.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        FeedService feedService = new FeedService(feedRepository, interactionRepository, eventPublisher, restTemplate, "", "", "contact", 72, 200);

        UUID feedId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        Feed feed = new Feed(feedId, authorId, "hello");

        FeedInteraction like = new FeedInteraction(UUID.randomUUID(), feedId, viewerId, FeedInteraction.Type.LIKE, null);
        FeedInteraction comment = new FeedInteraction(UUID.randomUUID(), feedId, UUID.randomUUID(), FeedInteraction.Type.COMMENT, "nice");
        when(interactionRepository.findByFeedIdIn(anyList())).thenReturn(Arrays.asList(like, comment));

        List<FeedResponse> responses = feedService.buildResponses(List.of(feed), viewerId);
        assertEquals(1, responses.size());
        FeedResponse response = responses.get(0);
        assertEquals(1, response.getLikeCount());
        assertEquals(1, response.getCommentCount());
        assertEquals(true, response.isLikedByViewer());
    }
}
