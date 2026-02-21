package com.tarotalk.event.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceAggregateResponse {
    private String traceId;
    private List<EventResponse> events;
    private Map<String, Long> eventTypeCounts = new HashMap<>();
    private Map<String, Long> sourceServiceCounts = new HashMap<>();
    private List<Map<String, Object>> causalEdges = new java.util.ArrayList<>();

    public TraceAggregateResponse(String traceId, List<EventResponse> events) {
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
}

