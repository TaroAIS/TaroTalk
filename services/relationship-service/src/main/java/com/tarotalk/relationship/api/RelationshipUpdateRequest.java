package com.tarotalk.relationship.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class RelationshipUpdateRequest {
    @NotBlank
    private String targetId;
    @NotBlank
    private String type;
    @NotNull
    private Double intimacyScore;
    private Long interactionCount;
    private Double commercialScore;

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getIntimacyScore() {
        return intimacyScore;
    }

    public void setIntimacyScore(Double intimacyScore) {
        this.intimacyScore = intimacyScore;
    }

    public Long getInteractionCount() {
        return interactionCount;
    }

    public void setInteractionCount(Long interactionCount) {
        this.interactionCount = interactionCount;
    }

    public Double getCommercialScore() {
        return commercialScore;
    }

    public void setCommercialScore(Double commercialScore) {
        this.commercialScore = commercialScore;
    }
}
