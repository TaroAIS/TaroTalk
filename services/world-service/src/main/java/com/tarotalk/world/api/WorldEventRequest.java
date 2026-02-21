package com.tarotalk.world.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;

public class WorldEventRequest {
    @NotBlank
    private String eventType;
    private String actorId;
    private String traceId;
    private Object payload;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @JsonProperty("payload_json")
    public Object getPayloadAlias() {
        return payload;
    }

    @JsonProperty("payload_json")
    public void setPayloadAlias(Object payloadJson) {
        this.payload = payloadJson;
    }
}
