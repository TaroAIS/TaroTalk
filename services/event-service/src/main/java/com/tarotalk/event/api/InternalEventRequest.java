package com.tarotalk.event.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import java.time.Instant;

public class InternalEventRequest {
    @NotBlank
    private String eventType;
    private String actorId;
    private String targetId;
    private String entityType;
    private String entityId;
    private String sourceService;
    private String idempotencyKey;
    private String traceId;
    private Object payload;
    private Instant createdAt;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @JsonProperty("payload_json")
    public Object getPayloadJsonAlias() {
        return payload;
    }

    @JsonProperty("payload_json")
    public void setPayloadJsonAlias(Object payloadJson) {
        this.payload = payloadJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
