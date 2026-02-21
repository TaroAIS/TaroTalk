package com.tarotalk.event.api;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Validated
public class EventController {
    private final EventService eventService;

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
        return ApiResponse.ok(response);
    }
}
