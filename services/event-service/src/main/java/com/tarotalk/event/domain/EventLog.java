package com.tarotalk.event.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "event_log",
        indexes = {
                @Index(name = "idx_event_trace_created", columnList = "trace_id,created_at"),
                @Index(name = "idx_event_entity_created", columnList = "entity_type,entity_id,created_at"),
                @Index(name = "idx_event_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_source_idempotency", columnNames = {"source_service", "idempotency_key"})
        }
)
public class EventLog {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "actor_id", length = 64)
    private String actorId;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "source_service", length = 64)
    private String sourceService;

    @Column(name = "idempotency_key", length = 180)
    private String idempotencyKey;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public EventLog() {
    }

    public EventLog(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.createdAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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
