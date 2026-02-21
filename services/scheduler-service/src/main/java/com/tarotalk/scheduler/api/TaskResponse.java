package com.tarotalk.scheduler.api;

import com.tarotalk.scheduler.domain.ScheduledTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskResponse {
    private UUID taskId;
    private String taskType;
    private String targetId;
    private String worldId;
    private String triggerType;
    private String objective;
    private List<String> actors = new ArrayList<>();
    private Integer priority;
    private String idempotencyKey;
    private boolean enabled;
    private String scheduleExpr;
    private String status;
    private Integer retryCount;
    private String lastError;
    private Instant lastRunAt;
    private Instant nextRunAt;
    private Instant updatedAt;

    public static TaskResponse from(ScheduledTask task, List<String> actors) {
        TaskResponse response = new TaskResponse();
        response.taskId = task.getTaskId();
        response.taskType = task.getTaskType();
        response.targetId = task.getTargetId();
        response.worldId = task.getWorldId();
        response.triggerType = task.getTriggerType();
        response.objective = task.getObjective();
        response.actors = actors == null ? new ArrayList<>() : actors;
        response.priority = task.getPriority();
        response.idempotencyKey = task.getIdempotencyKey();
        response.enabled = task.isEnabled();
        response.scheduleExpr = task.getScheduleExpr();
        response.status = task.getStatus() == null ? null : task.getStatus().name();
        response.retryCount = task.getRetryCount();
        response.lastError = task.getLastError();
        response.lastRunAt = task.getLastRunAt();
        response.nextRunAt = task.getNextRunAt();
        response.updatedAt = task.getUpdatedAt();
        return response;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getWorldId() {
        return worldId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getObjective() {
        return objective;
    }

    public List<String> getActors() {
        return actors;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getScheduleExpr() {
        return scheduleExpr;
    }

    public String getStatus() {
        return status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

