package com.tarotalk.event;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.event.api.EventController;
import com.tarotalk.event.api.TraceAggregateResponse;
import com.tarotalk.event.api.TraceExplainResponse;
import com.tarotalk.event.domain.EventLog;
import com.tarotalk.event.service.EventService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventControllerTest {
    @Test
    void replayAggregateIncludesCausalEdges() {
        EventService eventService = mock(EventService.class);
        EventController controller = new EventController(eventService);

        EventLog first = new EventLog(UUID.randomUUID(), "CHAT_MESSAGE_CREATED");
        first.setSourceService("chat-service");
        first.setTraceId("trace-1");
        first.setCreatedAt(Instant.now().minusSeconds(10));

        EventLog second = new EventLog(UUID.randomUUID(), "FEED_CREATED");
        second.setSourceService("feed-service");
        second.setTraceId("trace-1");
        second.setCreatedAt(Instant.now());

        when(eventService.replay("trace-1")).thenReturn(Arrays.asList(first, second));

        ApiResponse<TraceAggregateResponse> response = controller.replayAggregate("trace-1");
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getCausalEdges().size());
        Map<String, Object> edge = response.getData().getCausalEdges().get(0);
        assertEquals(first.getEventId().toString(), edge.get("cause_event_id"));
        assertEquals(second.getEventId().toString(), edge.get("effect_event_id"));
        assertEquals("TRACE_SEQUENCE", edge.get("relation_type"));
    }

    @Test
    void explainIncludesDebugChannels() {
        EventService eventService = mock(EventService.class);
        EventController controller = new EventController(eventService);

        EventLog row = new EventLog(UUID.randomUUID(), "ORCHESTRATOR_TURN");
        row.setSourceService("orchestrator");
        row.setTraceId("trace-x");
        row.setPayloadJson("{\"director_trace\":{\"step\":\"select\"},\"tool_calls\":[{\"name\":\"post_feed\"}],\"state_effects\":[{\"type\":\"MESSAGE\"}],\"bandit_decisions\":[{\"feedId\":\"f1\"}],\"drift_decisions\":[{\"role\":\"friend\"}],\"safety_report\":[{\"severity\":\"warn\"}]}");
        row.setCreatedAt(Instant.now());

        when(eventService.replay("trace-x")).thenReturn(Arrays.asList(row));

        ApiResponse<TraceExplainResponse> response = controller.explain("trace-x");
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getDirectorTrace().size());
        assertEquals(1, response.getData().getToolCalls().size());
        assertEquals(1, response.getData().getStateEffects().size());
        assertEquals(1, response.getData().getBanditDecisions().size());
        assertEquals(1, response.getData().getDriftDecisions().size());
        assertEquals(1, response.getData().getSafetyReport().size());
    }
}
