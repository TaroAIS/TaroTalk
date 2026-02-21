package com.tarotalk.world.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "world_causal_edge",
        indexes = {
                @Index(name = "idx_causal_world_created", columnList = "world_id,created_at"),
                @Index(name = "idx_causal_trace_created", columnList = "trace_id,created_at"),
                @Index(name = "idx_causal_world_cause", columnList = "world_id,cause_event_id"),
                @Index(name = "idx_causal_world_effect", columnList = "world_id,effect_event_id")
        }
)
public class WorldCausalEdge {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "edge_id", nullable = false, updatable = false)
    private UUID edgeId;

    @Type(type = "uuid-char")
    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "cause_event_id", nullable = false, length = 64)
    private String causeEventId;

    @Column(name = "effect_event_id", nullable = false, length = 64)
    private String effectEventId;

    @Column(name = "relation_type", nullable = false, length = 64)
    private String relationType;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WorldCausalEdge() {
    }

    public WorldCausalEdge(UUID edgeId,
                           UUID worldId,
                           String causeEventId,
                           String effectEventId,
                           String relationType,
                           Double confidence,
                           String traceId) {
        this.edgeId = edgeId;
        this.worldId = worldId;
        this.causeEventId = causeEventId;
        this.effectEventId = effectEventId;
        this.relationType = relationType;
        this.confidence = confidence;
        this.traceId = traceId;
        this.createdAt = Instant.now();
    }

    public UUID getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(UUID edgeId) {
        this.edgeId = edgeId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public String getCauseEventId() {
        return causeEventId;
    }

    public void setCauseEventId(String causeEventId) {
        this.causeEventId = causeEventId;
    }

    public String getEffectEventId() {
        return effectEventId;
    }

    public void setEffectEventId(String effectEventId) {
        this.effectEventId = effectEventId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
