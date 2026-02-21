package com.tarotalk.event.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceExplainResponse {
    private String traceId;
    private List<EventResponse> events;
    private Map<String, Long> eventTypeCounts = new HashMap<>();
    private Map<String, Long> sourceServiceCounts = new HashMap<>();
    private List<Map<String, Object>> causalEdges = new ArrayList<>();
    private List<Object> directorTrace = new ArrayList<>();
    private List<Object> toolCalls = new ArrayList<>();
    private List<Object> stateEffects = new ArrayList<>();
    private List<Object> banditDecisions = new ArrayList<>();
    private List<Object> driftDecisions = new ArrayList<>();
    private List<Object> safetyReport = new ArrayList<>();

    public TraceExplainResponse(String traceId, List<EventResponse> events) {
        this.traceId = traceId;
        this.events = events;
    }

    public String getTraceId() {
        return traceId;
    }

    public List<EventResponse> getEvents() {
        return events;
    }

    public Map<String, Long> getEventTypeCounts() {
        return eventTypeCounts;
    }

    public void setEventTypeCounts(Map<String, Long> eventTypeCounts) {
        this.eventTypeCounts = eventTypeCounts;
    }

    public Map<String, Long> getSourceServiceCounts() {
        return sourceServiceCounts;
    }

    public void setSourceServiceCounts(Map<String, Long> sourceServiceCounts) {
        this.sourceServiceCounts = sourceServiceCounts;
    }

    public List<Map<String, Object>> getCausalEdges() {
        return causalEdges;
    }

    public void setCausalEdges(List<Map<String, Object>> causalEdges) {
        this.causalEdges = causalEdges;
    }

    public List<Object> getDirectorTrace() {
        return directorTrace;
    }

    public void setDirectorTrace(List<Object> directorTrace) {
        this.directorTrace = directorTrace;
    }

    public List<Object> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<Object> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public List<Object> getStateEffects() {
        return stateEffects;
    }

    public void setStateEffects(List<Object> stateEffects) {
        this.stateEffects = stateEffects;
    }

    public List<Object> getBanditDecisions() {
        return banditDecisions;
    }

    public void setBanditDecisions(List<Object> banditDecisions) {
        this.banditDecisions = banditDecisions;
    }

    public List<Object> getDriftDecisions() {
        return driftDecisions;
    }

    public void setDriftDecisions(List<Object> driftDecisions) {
        this.driftDecisions = driftDecisions;
    }

    public List<Object> getSafetyReport() {
        return safetyReport;
    }

    public void setSafetyReport(List<Object> safetyReport) {
        this.safetyReport = safetyReport;
    }
}
