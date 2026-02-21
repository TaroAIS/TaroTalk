package com.tarotalk.world.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "story_arc")
public class StoryArc {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "arc_id", nullable = false, updatable = false)
    private UUID arcId;

    @Column(name = "world_id", nullable = false)
    @Type(type = "uuid-char")
    private UUID worldId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "progress", nullable = false)
    private double progress;

    @Lob
    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StoryArc() {
    }

    public StoryArc(UUID arcId, UUID worldId, String title) {
        this.arcId = arcId;
        this.worldId = worldId;
        this.title = title;
        this.status = "ACTIVE";
        this.progress = 0.0;
        this.metadataJson = "{}";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getArcId() {
        return arcId;
    }

    public void setArcId(UUID arcId) {
        this.arcId = arcId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
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
