package com.tarotalk.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.event.api.InternalEventRequest;
import com.tarotalk.event.domain.EventLog;
import com.tarotalk.event.repo.EventLogRepository;
import com.tarotalk.event.service.EventService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventServiceTest {
    @Test
    void appendReturnsExistingWhenIdempotencyKeyExists() {
        EventLogRepository repository = mock(EventLogRepository.class);
        EventService service = new EventService(repository, new ObjectMapper());

        EventLog existing = new EventLog(UUID.randomUUID(), "FEED_CREATED");
        existing.setSourceService("feed-service");
        existing.setIdempotencyKey("FEED_CREATED:1:2");
        when(repository.findFirstBySourceServiceAndIdempotencyKey("feed-service", "FEED_CREATED:1:2"))
                .thenReturn(Optional.of(existing));

        InternalEventRequest request = new InternalEventRequest();
        request.setEventType("FEED_CREATED");
        request.setSourceService("feed-service");
        request.setIdempotencyKey("FEED_CREATED:1:2");

        EventLog result = service.append(request);
        assertEquals(existing.getEventId(), result.getEventId());
        verify(repository, never()).save(any(EventLog.class));
    }
}

