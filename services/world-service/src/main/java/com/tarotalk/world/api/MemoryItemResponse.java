package com.tarotalk.world.api;

import com.tarotalk.world.domain.MemoryItem;

import java.time.Instant;
import java.util.UUID;

public class MemoryItemResponse {
    private UUID memoryId;
    private UUID worldId;
    private String ownerId;
    private String sourceEventId;
    private String summary;
    private Double salience;
    private Double decayRate;
    private String embeddingRef;
    private String tagsJson;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static MemoryItemResponse from(MemoryItem memoryItem) {
        MemoryItemResponse response = new MemoryItemResponse();
        response.memoryId = memoryItem.getMemoryId();
        response.worldId = memoryItem.getWorldId();
        response.ownerId = memoryItem.getOwnerId();
        response.sourceEventId = memoryItem.getSourceEventId();
        response.summary = memoryItem.getSummary();
        response.salience = memoryItem.getSalience();
        response.decayRate = memoryItem.getDecayRate();
        response.embeddingRef = memoryItem.getEmbeddingRef();
        response.tagsJson = memoryItem.getTagsJson();
        response.expiresAt = memoryItem.getExpiresAt();
        response.createdAt = memoryItem.getCreatedAt();
        response.updatedAt = memoryItem.getUpdatedAt();
        return response;
    }

    public UUID getMemoryId() {
        return memoryId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public String getSummary() {
        return summary;
    }

    public Double getSalience() {
        return salience;
    }

    public Double getDecayRate() {
        return decayRate;
    }

    public String getEmbeddingRef() {
        return embeddingRef;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
