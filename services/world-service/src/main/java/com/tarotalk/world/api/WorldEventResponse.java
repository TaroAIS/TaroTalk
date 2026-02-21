package com.tarotalk.world.api;

import com.tarotalk.world.domain.WorldEvent;

import java.time.Instant;
import java.util.UUID;

public class WorldEventResponse {
    private UUID eventId;
    private UUID worldId;
    private String eventType;
    private String actorId;
    private String traceId;
    private String payloadJson;
    private Instant createdAt;

    public static WorldEventResponse from(WorldEvent event) {
        WorldEventResponse response = new WorldEventResponse();
        response.eventId = event.getEventId();
        response.worldId = event.getWorldId();
        response.eventType = event.getEventType();
        response.actorId = event.getActorId();
        response.traceId = event.getTraceId();
        response.payloadJson = event.getPayloadJson();
        response.createdAt = event.getCreatedAt();
        return response;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActorId() {
        return actorId;
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
