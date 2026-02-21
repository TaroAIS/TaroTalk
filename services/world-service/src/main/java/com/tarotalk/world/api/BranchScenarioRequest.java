package com.tarotalk.world.api;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BranchScenarioRequest {
    private String traceId;
    private String selectionPolicy;
    private Boolean dryRun = Boolean.TRUE;

    @Valid
    @NotNull
    private List<BranchInput> branches = new ArrayList<>();

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSelectionPolicy() {
        return selectionPolicy;
    }

    public void setSelectionPolicy(String selectionPolicy) {
        this.selectionPolicy = selectionPolicy;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public List<BranchInput> getBranches() {
        return branches;
    }

    public void setBranches(List<BranchInput> branches) {
        this.branches = branches;
    }

    public static class BranchInput {
        @NotNull
        private String branchId;

        @NotNull
        private Double score;

        private String reason;
        private List<Map<String, Object>> events = new ArrayList<>();

        public String getBranchId() {
            return branchId;
        }

        public void setBranchId(String branchId) {
            this.branchId = branchId;
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

        public List<Map<String, Object>> getEvents() {
            return events;
        }

        public void setEvents(List<Map<String, Object>> events) {
            this.events = events;
        }
    }
}
