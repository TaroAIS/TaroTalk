package com.tarotalk.world.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GoalEconomyEvaluateResponse {
    private UUID worldId;
    private String objective;
    private Instant evaluatedAt;
    private List<GoalEconomyResponse> goals;

    public GoalEconomyEvaluateResponse(UUID worldId,
                                       String objective,
                                       Instant evaluatedAt,
                                       List<GoalEconomyResponse> goals) {
        this.worldId = worldId;
        this.objective = objective;
        this.evaluatedAt = evaluatedAt;
        this.goals = goals;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getObjective() {
        return objective;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public List<GoalEconomyResponse> getGoals() {
        return goals;
    }
}
