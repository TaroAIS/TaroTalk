package com.tarotalk.scheduler.api;

import javax.validation.constraints.NotBlank;

public class TaskRequest {
    @NotBlank
    private String taskType;
    private String targetId;
    private String payload;

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
}
