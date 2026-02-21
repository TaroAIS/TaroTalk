package com.tarotalk.world.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RecomputeGoalsResponse {
    private UUID worldId;
    private Instant recomputedAt;
    private List<GoalResponse> goals;

    public RecomputeGoalsResponse() {
    }

    public RecomputeGoalsResponse(UUID worldId, Instant recomputedAt, List<GoalResponse> goals) {
        this.worldId = worldId;
        this.recomputedAt = recomputedAt;
        this.goals = goals;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public Instant getRecomputedAt() {
        return recomputedAt;
    }

    public void setRecomputedAt(Instant recomputedAt) {
        this.recomputedAt = recomputedAt;
    }

    public List<GoalResponse> getGoals() {
        return goals;
    }

    public void setGoals(List<GoalResponse> goals) {
        this.goals = goals;
    }
}
