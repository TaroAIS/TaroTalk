package com.tarotalk.scheduler.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "scheduled_tasks",
        indexes = {
                @Index(name = "idx_scheduled_task_enabled_next_run", columnList = "enabled,next_run_at"),
                @Index(name = "idx_scheduled_task_status_next_run", columnList = "status,next_run_at")
        }
)
public class ScheduledTask {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "task_type", nullable = false, length = 80)
    private String taskType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "payload", length = 2000)
    private String payload;

    @Column(name = "world_id", length = 64)
    private String worldId;

    @Column(name = "trigger_type", length = 64)
    private String triggerType;

    @Column(name = "objective", length = 500)
    private String objective;

    @Column(name = "actors_json", length = 2000)
    private String actorsJson;

    @Column(name = "priority_value")
    private Integer priority;

    @Column(name = "idempotency_key", length = 120, unique = true)
    private String idempotencyKey;

    @Column(name = "schedule_expr", length = 120)
    private String scheduleExpr;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ScheduledTask() {
    }

    public ScheduledTask(UUID taskId, String taskType) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.enabled = true;
        this.status = TaskStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

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

    public String getActorsJson() {
        return actorsJson;
    }

    public void setActorsJson(String actorsJson) {
        this.actorsJson = actorsJson;
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

    public String getScheduleExpr() {
        return scheduleExpr;
    }

    public void setScheduleExpr(String scheduleExpr) {
        this.scheduleExpr = scheduleExpr;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
