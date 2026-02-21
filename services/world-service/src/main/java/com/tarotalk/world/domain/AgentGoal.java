package com.tarotalk.world.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_goal")
public class AgentGoal {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "goal_id", nullable = false, updatable = false)
    private UUID goalId;

    @Column(name = "world_id", nullable = false)
    @Type(type = "uuid-char")
    private UUID worldId;

    @Column(name = "agent_id", nullable = false, length = 64)
    private String agentId;

    @Column(name = "goal_type", nullable = false, length = 64)
    private String goalType;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "budget", nullable = false)
    private double budget;

    @Column(name = "expected_reward", nullable = false)
    private double expectedReward;

    @Column(name = "risk_penalty", nullable = false)
    private double riskPenalty;

    @Column(name = "momentum", nullable = false)
    private double momentum;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AgentGoal() {
    }

    public AgentGoal(UUID goalId, UUID worldId, String agentId, String goalType, int priority) {
        this.goalId = goalId;
        this.worldId = worldId;
        this.agentId = agentId;
        this.goalType = goalType;
        this.priority = priority;
        this.status = "ACTIVE";
        this.score = 0.0;
        this.budget = 1.0;
        this.expectedReward = 0.3;
        this.riskPenalty = 0.1;
        this.momentum = 0.2;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public void setGoalId(UUID goalId) {
        this.goalId = goalId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getGoalType() {
        return goalType;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public double getExpectedReward() {
        return expectedReward;
    }

    public void setExpectedReward(double expectedReward) {
        this.expectedReward = expectedReward;
    }

    public double getRiskPenalty() {
        return riskPenalty;
    }

    public void setRiskPenalty(double riskPenalty) {
        this.riskPenalty = riskPenalty;
    }

    public double getMomentum() {
        return momentum;
    }

    public void setMomentum(double momentum) {
        this.momentum = momentum;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
