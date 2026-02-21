package com.tarotalk.world.api;

import com.tarotalk.world.domain.WorldCausalEdge;

import java.time.Instant;
import java.util.UUID;

public class CausalEdgeResponse {
    private UUID edgeId;
    private UUID worldId;
    private String causeEventId;
    private String effectEventId;
    private String relationType;
    private Double confidence;
    private String traceId;
    private Instant createdAt;

    public static CausalEdgeResponse from(WorldCausalEdge edge) {
        CausalEdgeResponse response = new CausalEdgeResponse();
        response.edgeId = edge.getEdgeId();
        response.worldId = edge.getWorldId();
        response.causeEventId = edge.getCauseEventId();
        response.effectEventId = edge.getEffectEventId();
        response.relationType = edge.getRelationType();
        response.confidence = edge.getConfidence();
        response.traceId = edge.getTraceId();
        response.createdAt = edge.getCreatedAt();
        return response;
    }

    public UUID getEdgeId() {
        return edgeId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getCauseEventId() {
        return causeEventId;
    }

    public String getEffectEventId() {
        return effectEventId;
    }

    public String getRelationType() {
        return relationType;
    }

    public Double getConfidence() {
        return confidence;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
