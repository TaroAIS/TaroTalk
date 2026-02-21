package com.tarotalk.world.api;

import com.tarotalk.world.domain.WorldState;

import java.time.Instant;
import java.util.UUID;

public class WorldStateResponse {
    private UUID worldId;
    private String snapshotJson;
    private long version;
    private Instant updatedAt;

    public static WorldStateResponse from(WorldState state) {
        WorldStateResponse response = new WorldStateResponse();
        response.worldId = state.getWorldId();
        response.snapshotJson = state.getSnapshotJson();
        response.version = state.getVersion();
        response.updatedAt = state.getUpdatedAt();
        return response;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
