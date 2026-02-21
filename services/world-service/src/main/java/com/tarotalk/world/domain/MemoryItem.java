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
        name = "memory_item",
        indexes = {
                @Index(name = "idx_memory_world_owner_salience", columnList = "world_id,owner_id,salience"),
                @Index(name = "idx_memory_world_expires", columnList = "world_id,expires_at"),
                @Index(name = "idx_memory_world_source_hash", columnList = "world_id,owner_id,source_event_id,summary_hash")
        }
)
public class MemoryItem {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Type(type = "uuid-char")
    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "owner_id", length = 64, nullable = false)
    private String ownerId;

    @Column(name = "source_event_id", length = 64, nullable = false)
    private String sourceEventId;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "summary_hash", nullable = false, length = 64)
    private String summaryHash;

    @Column(name = "salience", nullable = false)
    private Double salience;

    @Column(name = "decay_rate", nullable = false)
    private Double decayRate;

    @Column(name = "embedding_ref", length = 128)
    private String embeddingRef;

    @Lob
    @Column(name = "tags_json")
    private String tagsJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MemoryItem() {
    }

    public MemoryItem(UUID memoryId, UUID worldId, String ownerId, String sourceEventId, String summary) {
        this.memoryId = memoryId;
        this.worldId = worldId;
        this.ownerId = ownerId;
        this.sourceEventId = sourceEventId;
        this.summary = summary;
        this.salience = 0.5;
        this.decayRate = 0.05;
        this.tagsJson = "{}";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(7 * 24 * 3600L);
    }

    public UUID getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(UUID memoryId) {
        this.memoryId = memoryId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummaryHash() {
        return summaryHash;
    }

    public void setSummaryHash(String summaryHash) {
        this.summaryHash = summaryHash;
    }

    public Double getSalience() {
        return salience;
    }

    public void setSalience(Double salience) {
        this.salience = salience;
    }

    public Double getDecayRate() {
        return decayRate;
    }

    public void setDecayRate(Double decayRate) {
        this.decayRate = decayRate;
    }

    public String getEmbeddingRef() {
        return embeddingRef;
    }

    public void setEmbeddingRef(String embeddingRef) {
        this.embeddingRef = embeddingRef;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
