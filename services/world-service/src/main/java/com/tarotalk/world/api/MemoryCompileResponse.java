package com.tarotalk.world.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MemoryCompileResponse {
    private UUID worldId;
    private String ownerId;
    private int createdCount;
    private int deduplicatedCount;
    private int returnedCount;
    private Instant compiledAt;
    private List<MemoryItemResponse> memories;

    public MemoryCompileResponse() {
    }

    public MemoryCompileResponse(UUID worldId,
                                 String ownerId,
                                 int createdCount,
                                 int deduplicatedCount,
                                 int returnedCount,
                                 Instant compiledAt,
                                 List<MemoryItemResponse> memories) {
        this.worldId = worldId;
        this.ownerId = ownerId;
        this.createdCount = createdCount;
        this.deduplicatedCount = deduplicatedCount;
        this.returnedCount = returnedCount;
        this.compiledAt = compiledAt;
        this.memories = memories;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public int getDeduplicatedCount() {
        return deduplicatedCount;
    }

    public int getReturnedCount() {
        return returnedCount;
    }

    public Instant getCompiledAt() {
        return compiledAt;
    }

    public List<MemoryItemResponse> getMemories() {
        return memories;
    }
}
