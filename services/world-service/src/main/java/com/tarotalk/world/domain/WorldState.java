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
@Table(name = "world_state")
public class WorldState {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Lob
    @Column(name = "snapshot_json")
    private String snapshotJson;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WorldState() {
    }

    public WorldState(UUID worldId) {
        this.worldId = worldId;
        this.snapshotJson = "{}";
        this.version = 0L;
        this.updatedAt = Instant.now();
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
