package com.tarotalk.relationship.api;

public class RelationshipResponse {
    private String targetId;
    private String type;
    private double intimacyScore;
    private long interactionCount;
    private double commercialScore;

    public RelationshipResponse() {
    }

    public RelationshipResponse(String targetId, String type, double intimacyScore, long interactionCount, double commercialScore) {
        this.targetId = targetId;
        this.type = type;
        this.intimacyScore = intimacyScore;
        this.interactionCount = interactionCount;
        this.commercialScore = commercialScore;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getType() {
        return type;
    }

    public double getIntimacyScore() {
        return intimacyScore;
    }

    public long getInteractionCount() {
        return interactionCount;
    }

    public double getCommercialScore() {
        return commercialScore;
    }
}
