package com.tarotalk.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.feed.domain.Feed;
import com.tarotalk.feed.service.FeedEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FeedEventPublisherTest {
    @Test
    void publishFeedCreatedSendsNotifications() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        FeedEventPublisher publisher = new FeedEventPublisher(restTemplate, objectMapper, "http://user", "http://notify");

        UUID authorId = UUID.randomUUID();
        UUID feedId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("data", List.of(contactId.toString()));
        when(restTemplate.getForObject(eq("http://user/api/contacts/ids?userId=" + authorId), eq(Map.class)))
                .thenReturn(apiResponse);

        Feed feed = new Feed(feedId, authorId, "Hello feed");
        publisher.publishFeedCreated(feed);

        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restTemplate, times(1))
                .postForObject(eq("http://notify/api/notifications"), payloadCaptor.capture(), eq(Map.class));
        assertEquals("FEED_CREATED", payloadCaptor.getValue().get("type"));
    }

    @Test
    void publishFeedLikedNotifiesAuthor() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        FeedEventPublisher publisher = new FeedEventPublisher(restTemplate, objectMapper, "http://user", "http://notify");

        UUID authorId = UUID.randomUUID();
        UUID feedId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Feed feed = new Feed(feedId, authorId, "Hello feed");

        publisher.publishFeedLiked(feed, actorId);

        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restTemplate, times(1))
                .postForObject(eq("http://notify/api/notifications"), payloadCaptor.capture(), eq(Map.class));
        assertEquals("FEED_LIKED", payloadCaptor.getValue().get("type"));
    }
}
