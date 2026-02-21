package com.tarotalk.world.api;

import com.tarotalk.world.service.WorldService;

import java.time.Instant;
import java.util.UUID;

public class GoalEconomyResponse {
    private UUID goalId;
    private UUID worldId;
    private String agentId;
    private String goalType;
    private int priority;
    private String status;
    private double score;
    private double budget;
    private double expectedReward;
    private double riskPenalty;
    private double momentum;
    private double utility;
    private Instant updatedAt;

    public static GoalEconomyResponse from(WorldService.GoalEconomyEntry entry) {
        GoalEconomyResponse response = new GoalEconomyResponse();
        response.goalId = entry.getGoalId();
        response.worldId = entry.getWorldId();
        response.agentId = entry.getAgentId();
        response.goalType = entry.getGoalType();
        response.priority = entry.getPriority();
        response.status = entry.getStatus();
        response.score = entry.getScore();
        response.budget = entry.getBudget();
        response.expectedReward = entry.getExpectedReward();
        response.riskPenalty = entry.getRiskPenalty();
        response.momentum = entry.getMomentum();
        response.utility = entry.getUtility();
        response.updatedAt = entry.getUpdatedAt();
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

    public double getBudget() {
        return budget;
    }

    public double getExpectedReward() {
        return expectedReward;
    }

    public double getRiskPenalty() {
        return riskPenalty;
    }

    public double getMomentum() {
        return momentum;
    }

    public double getUtility() {
        return utility;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
