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
        name = "world_branch_scenario",
        indexes = {
                @Index(name = "idx_branch_world_created", columnList = "world_id,created_at"),
                @Index(name = "idx_branch_trace_created", columnList = "trace_id,created_at"),
                @Index(name = "idx_branch_world_branch", columnList = "world_id,branch_id")
        }
)
public class WorldBranchScenario {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "scenario_id", nullable = false, updatable = false)
    private UUID scenarioId;

    @Type(type = "uuid-char")
    @Column(name = "world_id", nullable = false)
    private UUID worldId;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "branch_id", nullable = false, length = 64)
    private String branchId;

    @Column(name = "selection_policy", length = 64)
    private String selectionPolicy;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "reason", length = 500)
    private String reason;

    @Lob
    @Column(name = "events_json")
    private String eventsJson;

    @Column(name = "dry_run", nullable = false)
    private Boolean dryRun;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WorldBranchScenario() {
    }

    public WorldBranchScenario(UUID scenarioId, UUID worldId, String branchId, Double score) {
        this.scenarioId = scenarioId;
        this.worldId = worldId;
        this.branchId = branchId;
        this.score = score;
        this.dryRun = Boolean.TRUE;
        this.createdAt = Instant.now();
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(UUID scenarioId) {
        this.scenarioId = scenarioId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getSelectionPolicy() {
        return selectionPolicy;
    }

    public void setSelectionPolicy(String selectionPolicy) {
        this.selectionPolicy = selectionPolicy;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEventsJson() {
        return eventsJson;
    }

    public void setEventsJson(String eventsJson) {
        this.eventsJson = eventsJson;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
