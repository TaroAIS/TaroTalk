package com.tarotalk.event.api;

import java.util.List;

public class TraceReplayResponse {
    private String traceId;
    private List<EventResponse> events;

    public TraceReplayResponse() {
    }

    public TraceReplayResponse(String traceId, List<EventResponse> events) {
        this.traceId = traceId;
        this.events = events;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public List<EventResponse> getEvents() {
        return events;
    }

    public void setEvents(List<EventResponse> events) {
        this.events = events;
    }
}
