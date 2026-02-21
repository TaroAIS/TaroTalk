package com.tarotalk.event.api;

import com.tarotalk.event.domain.EventLog;

import java.time.Instant;
import java.util.UUID;

public class EventWriteResponse {
    private UUID eventId;
    private String traceId;
    private String idempotencyKey;
    private String eventType;
    private Instant createdAt;

    public static EventWriteResponse from(EventLog eventLog) {
        EventWriteResponse response = new EventWriteResponse();
        response.eventId = eventLog.getEventId();
        response.traceId = eventLog.getTraceId();
        response.idempotencyKey = eventLog.getIdempotencyKey();
        response.eventType = eventLog.getEventType();
        response.createdAt = eventLog.getCreatedAt();
        return response;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
