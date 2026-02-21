package com.tarotalk.world.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "world_event",
        indexes = {
                @Index(name = "idx_world_event_world_created", columnList = "world_id,created_at"),
                @Index(name = "idx_world_event_trace_created", columnList = "trace_id,created_at")
        }
)
public class WorldEvent {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "world_id", nullable = false)
    @Type(type = "uuid-char")
    private UUID worldId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "actor_id", length = 64)
    private String actorId;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WorldEvent() {
    }

    public WorldEvent(UUID eventId, UUID worldId, String eventType) {
        this.eventId = eventId;
        this.worldId = worldId;
        this.eventType = eventType;
        this.createdAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
