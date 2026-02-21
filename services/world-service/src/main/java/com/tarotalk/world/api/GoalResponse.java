package com.tarotalk.world.api;

import com.tarotalk.world.domain.AgentGoal;

import java.time.Instant;
import java.util.UUID;

public class GoalResponse {
    private UUID goalId;
    private UUID worldId;
    private String agentId;
    private String goalType;
    private int priority;
    private String status;
    private double score;
    private Instant updatedAt;

    public static GoalResponse from(AgentGoal goal) {
        GoalResponse response = new GoalResponse();
        response.goalId = goal.getGoalId();
        response.worldId = goal.getWorldId();
        response.agentId = goal.getAgentId();
        response.goalType = goal.getGoalType();
        response.priority = goal.getPriority();
        response.status = goal.getStatus();
        response.score = goal.getScore();
        response.updatedAt = goal.getUpdatedAt();
        return response;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getGoalType() {
        return goalType;
    }

    public int getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public double getScore() {
        return score;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
