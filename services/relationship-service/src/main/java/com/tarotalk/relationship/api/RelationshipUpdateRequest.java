package com.tarotalk.relationship.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.tarotalk.common.exception.ApiException;

public class RelationshipUpdateRequest {
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "friend", "mentor", "rival", "advertiser", "self-agent"
    ));

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
        if (type == null) {
            this.type = null;
            return;
        }
        String normalized = type.trim().toLowerCase();
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new ApiException("VALIDATION_ERROR", "invalid relationship type");
        }
        this.type = normalized;
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
