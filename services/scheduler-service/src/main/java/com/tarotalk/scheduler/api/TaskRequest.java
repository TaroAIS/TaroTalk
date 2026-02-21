package com.tarotalk.scheduler.api;

import javax.validation.constraints.NotBlank;
import java.util.UUID;
import java.util.List;

public class TaskRequest {
    private UUID taskId;
    @NotBlank
    private String taskType;
    private String targetId;
    private String payload;
    private String worldId;
    private String triggerType;
    private String objective;
    private List<String> actors;
    private Integer priority;
    private String idempotencyKey;
    private Boolean enabled;
    private String scheduleExpr;
    private String status;

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getWorldId() {
        return worldId;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public List<String> getActors() {
        return actors;
    }

    public void setActors(List<String> actors) {
        this.actors = actors;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getScheduleExpr() {
        return scheduleExpr;
    }

    public void setScheduleExpr(String scheduleExpr) {
        this.scheduleExpr = scheduleExpr;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
