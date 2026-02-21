package com.tarotalk.world.api;

import com.tarotalk.world.domain.WorldBranchScenario;

import java.time.Instant;
import java.util.UUID;

public class BranchScenarioResponse {
    private UUID scenarioId;
    private UUID worldId;
    private String traceId;
    private String branchId;
    private String selectionPolicy;
    private Double score;
    private String reason;
    private String eventsJson;
    private Boolean dryRun;
    private Instant createdAt;

    public static BranchScenarioResponse from(WorldBranchScenario scenario) {
        BranchScenarioResponse response = new BranchScenarioResponse();
        response.scenarioId = scenario.getScenarioId();
        response.worldId = scenario.getWorldId();
        response.traceId = scenario.getTraceId();
        response.branchId = scenario.getBranchId();
        response.selectionPolicy = scenario.getSelectionPolicy();
        response.score = scenario.getScore();
        response.reason = scenario.getReason();
        response.eventsJson = scenario.getEventsJson();
        response.dryRun = scenario.getDryRun();
        response.createdAt = scenario.getCreatedAt();
        return response;
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getSelectionPolicy() {
        return selectionPolicy;
    }

    public Double getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }

    public String getEventsJson() {
        return eventsJson;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
