package com.tarotalk.feed.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class LikeV2Response {
    private UUID feedId;
    private UUID userId;
    private String action;
    private boolean liked;
    private String eventId;

    public LikeV2Response() {
    }

    public LikeV2Response(UUID feedId, UUID userId, String action, boolean liked, String eventId) {
        this.feedId = feedId;
        this.userId = userId;
        this.action = action;
        this.liked = liked;
        this.eventId = eventId;
    }

    public UUID getFeedId() {
        return feedId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public boolean isLiked() {
        return liked;
    }

    @JsonProperty("event_id")
    public String getEventId() {
        return eventId;
    }
}
