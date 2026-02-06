package com.tarotalk.feed.api;

import com.tarotalk.feed.domain.Feed;

import java.time.Instant;
import java.util.UUID;

public class FeedResponse {
    private UUID feedId;
    private UUID authorId;
    private String content;
    private String mediaUrls;
    private String location;
    private String topics;
    private Feed.Visibility visibility;
    private Instant createdAt;
    private long likeCount;
    private long commentCount;
    private boolean likedByViewer;

    public static FeedResponse from(Feed feed) {
        FeedResponse response = new FeedResponse();
        response.feedId = feed.getFeedId();
        response.authorId = feed.getAuthorId();
        response.content = feed.getContent();
        response.mediaUrls = feed.getMediaUrls();
        response.location = feed.getLocation();
        response.topics = feed.getTopics();
        response.visibility = feed.getVisibility();
        response.createdAt = feed.getCreatedAt();
        return response;
    }

    public static FeedResponse from(Feed feed, long likeCount, long commentCount, boolean likedByViewer) {
        FeedResponse response = from(feed);
        response.likeCount = likeCount;
        response.commentCount = commentCount;
        response.likedByViewer = likedByViewer;
        return response;
    }

    public UUID getFeedId() {
        return feedId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public String getMediaUrls() {
        return mediaUrls;
    }

    public String getLocation() {
        return location;
    }

    public String getTopics() {
        return topics;
    }

    public Feed.Visibility getVisibility() {
        return visibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public boolean isLikedByViewer() {
        return likedByViewer;
    }
}
