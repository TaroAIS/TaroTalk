package com.tarotalk.event.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.event.service.EventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Validated
public class EventController {
    private final EventService eventService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/internal/events")
    public ApiResponse<EventWriteResponse> append(@Valid @RequestBody InternalEventRequest request) {
        return ApiResponse.ok(EventWriteResponse.from(eventService.append(request)));
    }

    @GetMapping("/api/v2/events")
    public ApiResponse<List<EventResponse>> list(@RequestParam(required = false) String entityType,
                                                 @RequestParam(required = false) String entityId,
                                                 @RequestParam(required = false) String actorId,
                                                 @RequestParam(required = false) String targetId,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                 @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(eventService.query(entityType, entityId, actorId, targetId, from, to, limit)
                .stream()
                .map(EventResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/api/v2/traces/{traceId}/replay")
    public ApiResponse<TraceReplayResponse> replay(@PathVariable String traceId) {
        List<EventResponse> rows = eventService.replay(traceId)
                .stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.ok(new TraceReplayResponse(traceId, rows));
    }

    @GetMapping("/api/v2/traces/{traceId}/replay/aggregate")
    public ApiResponse<TraceAggregateResponse> replayAggregate(@PathVariable String traceId) {
        List<EventResponse> rows = eventService.replay(traceId)
                .stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
        TraceAggregateResponse response = buildAggregate(traceId, rows);
        return ApiResponse.ok(response);
    }

    @GetMapping("/api/v2/traces/{traceId}/explain")
    public ApiResponse<TraceExplainResponse> explain(@PathVariable String traceId) {
        List<EventResponse> rows = eventService.replay(traceId)
                .stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());

        TraceAggregateResponse aggregate = buildAggregate(traceId, rows);
        TraceExplainResponse response = new TraceExplainResponse(traceId, rows);
        response.setEventTypeCounts(aggregate.getEventTypeCounts());
        response.setSourceServiceCounts(aggregate.getSourceServiceCounts());
        response.setCausalEdges(aggregate.getCausalEdges());
        response.setDirectorTrace(extractValues(rows, "director_trace", "DIRECTOR_TRACE"));
        response.setToolCalls(extractValues(rows, "tool_calls", "TOOL_CALL"));
        response.setStateEffects(extractValues(rows, "state_effects", "STATE_EFFECT"));
        response.setBanditDecisions(extractValues(rows, "bandit_decisions", "BANDIT"));
        response.setDriftDecisions(extractDriftDecisions(rows));
        response.setSafetyReport(extractValues(rows, "safety_report", "SAFETY"));
        return ApiResponse.ok(response);
    }

    private TraceAggregateResponse buildAggregate(String traceId, List<EventResponse> rows) {
        TraceAggregateResponse response = new TraceAggregateResponse(traceId, rows);
        Map<String, Long> eventTypeCounts = new HashMap<>();
        Map<String, Long> sourceServiceCounts = new HashMap<>();
        for (EventResponse row : rows) {
            String eventType = row.getEventType() == null ? "UNKNOWN" : row.getEventType();
            eventTypeCounts.put(eventType, eventTypeCounts.getOrDefault(eventType, 0L) + 1L);
            String sourceService = row.getSourceService() == null ? "UNKNOWN" : row.getSourceService();
            sourceServiceCounts.put(sourceService, sourceServiceCounts.getOrDefault(sourceService, 0L) + 1L);
        }
        response.setEventTypeCounts(eventTypeCounts);
        response.setSourceServiceCounts(sourceServiceCounts);
        response.setCausalEdges(buildCausalEdges(traceId, rows));
        return response;
    }

    private List<Map<String, Object>> buildCausalEdges(String traceId, List<EventResponse> rows) {
        List<Map<String, Object>> edges = new java.util.ArrayList<>();
        if (rows == null || rows.size() < 2) {
            return edges;
        }
        for (int index = 0; index < rows.size() - 1; index++) {
            EventResponse cause = rows.get(index);
            EventResponse effect = rows.get(index + 1);
            Map<String, Object> edge = new HashMap<>();
            edge.put("trace_id", traceId);
            edge.put("cause_event_id", cause.getEventId() == null ? null : cause.getEventId().toString());
            edge.put("effect_event_id", effect.getEventId() == null ? null : effect.getEventId().toString());
            edge.put("relation_type", "TRACE_SEQUENCE");
            edge.put("confidence", 0.6);
            edges.add(edge);
        }
        return edges;
    }

    private List<Object> extractValues(List<EventResponse> rows, String key, String eventTypeHint) {
        List<Object> values = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return values;
        }
        for (EventResponse row : rows) {
            Map<String, Object> payload = parsePayload(row.getPayloadJson());
            appendValue(values, payload.get(key));
            if (eventTypeHint != null
                    && row.getEventType() != null
                    && row.getEventType().toUpperCase().contains(eventTypeHint)
                    && !payload.isEmpty()) {
                values.add(payload);
            }
        }
        return values;
    }

    private List<Object> extractDriftDecisions(List<EventResponse> rows) {
        List<Object> values = extractValues(rows, "drift_decisions", "DRIFT");
        for (EventResponse row : rows) {
            Map<String, Object> payload = parsePayload(row.getPayloadJson());
            Object directorTrace = payload.get("director_trace");
            if (directorTrace instanceof Map) {
                appendValue(values, ((Map<?, ?>) directorTrace).get("drift_decisions"));
            } else if (directorTrace instanceof List) {
                for (Object item : (List<?>) directorTrace) {
                    if (item instanceof Map) {
                        appendValue(values, ((Map<?, ?>) item).get("drift_decisions"));
                    }
                }
            }
        }
        return values;
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            Object parsed = objectMapper.readValue(payloadJson, Object.class);
            if (parsed instanceof Map) {
                Map<String, Object> normalized = new HashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) parsed).entrySet()) {
                    normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                return normalized;
            }
            return new HashMap<>();
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    private void appendValue(List<Object> bucket, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    bucket.add(item);
                }
            }
            return;
        }
        bucket.add(value);
    }
}
