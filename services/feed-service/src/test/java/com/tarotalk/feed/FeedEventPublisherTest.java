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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class FeedEventPublisherTest {
    @Test
    void publishFeedCreatedUsesHybridFanout() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        FeedEventPublisher publisher = new FeedEventPublisher(
                restTemplate,
                objectMapper,
                "http://user",
                "http://notify",
                "http://event",
                "http://rel",
                "http://world",
                "hybrid",
                60
        );

        UUID authorId = UUID.randomUUID();
        UUID feedId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        UUID worldActorId = UUID.randomUUID();

        Map<String, Object> contactResponse = new HashMap<>();
        contactResponse.put("data", List.of(contactId.toString()));
        when(restTemplate.getForObject(eq("http://user/api/contacts/ids?userId=" + authorId), eq(Map.class)))
                .thenReturn(contactResponse);

        Map<String, Object> relationRow = new HashMap<>();
        relationRow.put("targetId", relationId.toString());
        Map<String, Object> relationResponse = new HashMap<>();
        relationResponse.put("data", List.of(relationRow));
        when(restTemplate.getForObject(eq("http://rel/api/relationships/" + authorId), eq(Map.class)))
                .thenReturn(relationResponse);

        Map<String, Object> worldRow = new HashMap<>();
        worldRow.put("actorId", worldActorId.toString());
        Map<String, Object> worldResponse = new HashMap<>();
        worldResponse.put("data", List.of(worldRow));
        when(restTemplate.getForObject(eq("http://world/api/v2/worlds/" + authorId + "/timeline?limit=30"), eq(Map.class)))
                .thenReturn(worldResponse);

        Map<String, Object> eventWriteData = new HashMap<>();
        eventWriteData.put("eventId", UUID.randomUUID().toString());
        Map<String, Object> eventWriteResponse = new HashMap<>();
        eventWriteResponse.put("data", eventWriteData);
        when(restTemplate.postForObject(eq("http://event/internal/events"), any(Map.class), eq(Map.class)))
                .thenReturn(eventWriteResponse);

        Feed feed = new Feed(feedId, authorId, "Hello feed");
        publisher.publishFeedCreated(feed);

        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restTemplate, times(3))
                .postForObject(eq("http://notify/api/notifications"), payloadCaptor.capture(), eq(Map.class));
        List<Map> payloads = payloadCaptor.getAllValues();
        assertTrue(payloads.stream().allMatch(row -> "FEED_CREATED".equals(row.get("type"))));

        verify(restTemplate, times(1))
                .postForObject(eq("http://event/internal/events"), any(Map.class), eq(Map.class));
    }

    @Test
    void publishFeedLikedExcludesActorFromFanout() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        FeedEventPublisher publisher = new FeedEventPublisher(
                restTemplate,
                objectMapper,
                "http://user",
                "http://notify",
                "http://event",
                "http://rel",
                "http://world",
                "hybrid",
                60
        );

        UUID authorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        UUID feedId = UUID.randomUUID();

        Map<String, Object> contactResponse = new HashMap<>();
        contactResponse.put("data", List.of(actorId.toString()));
        when(restTemplate.getForObject(eq("http://user/api/contacts/ids?userId=" + authorId), eq(Map.class)))
                .thenReturn(contactResponse);

        Map<String, Object> relationRow = new HashMap<>();
        relationRow.put("targetId", relationId.toString());
        Map<String, Object> relationResponse = new HashMap<>();
        relationResponse.put("data", List.of(relationRow));
        when(restTemplate.getForObject(eq("http://rel/api/relationships/" + authorId), eq(Map.class)))
                .thenReturn(relationResponse);

        Map<String, Object> worldResponse = new HashMap<>();
        worldResponse.put("data", List.of());
        when(restTemplate.getForObject(eq("http://world/api/v2/worlds/" + authorId + "/timeline?limit=30"), eq(Map.class)))
                .thenReturn(worldResponse);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", UUID.randomUUID().toString());
        Map<String, Object> eventWriteResponse = new HashMap<>();
        eventWriteResponse.put("data", eventData);
        when(restTemplate.postForObject(eq("http://event/internal/events"), any(Map.class), eq(Map.class)))
                .thenReturn(eventWriteResponse);

        Feed feed = new Feed(feedId, authorId, "Hello feed");
        publisher.publishFeedLiked(feed, actorId);

        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(restTemplate, times(2))
                .postForObject(eq("http://notify/api/notifications"), payloadCaptor.capture(), eq(Map.class));

        List<Map> payloads = payloadCaptor.getAllValues();
        long actorNotifications = payloads.stream()
                .filter(row -> actorId.toString().equals(String.valueOf(row.get("userId"))))
                .count();
        assertEquals(0L, actorNotifications);
        assertTrue(payloads.stream().allMatch(row -> "FEED_LIKED".equals(row.get("type"))));
    }
}
