package com.tarotalk.event.api;

import com.tarotalk.event.domain.EventLog;

import java.time.Instant;
import java.util.UUID;

public class EventResponse {
    private UUID eventId;
    private String eventType;
    private String actorId;
    private String targetId;
    private String entityType;
    private String entityId;
    private String sourceService;
    private String idempotencyKey;
    private String traceId;
    private String payloadJson;
    private Instant createdAt;

    public static EventResponse from(EventLog eventLog) {
        EventResponse response = new EventResponse();
        response.eventId = eventLog.getEventId();
        response.eventType = eventLog.getEventType();
        response.actorId = eventLog.getActorId();
        response.targetId = eventLog.getTargetId();
        response.entityType = eventLog.getEntityType();
        response.entityId = eventLog.getEntityId();
        response.sourceService = eventLog.getSourceService();
        response.idempotencyKey = eventLog.getIdempotencyKey();
        response.traceId = eventLog.getTraceId();
        response.payloadJson = eventLog.getPayloadJson();
        response.createdAt = eventLog.getCreatedAt();
        return response;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActorId() {
        return actorId;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getSourceService() {
        return sourceService;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
