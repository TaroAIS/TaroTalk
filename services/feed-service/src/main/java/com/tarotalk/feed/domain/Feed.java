package com.tarotalk.feed.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "feeds",
        indexes = {
                @Index(name = "idx_feed_created_at", columnList = "created_at"),
                @Index(name = "idx_feed_author_created", columnList = "author_id,created_at")
        }
)
public class Feed {
    public enum Visibility {
        PUBLIC,
        FRIENDS,
        PRIVATE
    }

    @Id
    @Type(type = "uuid-char")
    @Column(name = "feed_id", nullable = false, updatable = false)
    private UUID feedId;

    @Column(name = "author_id", nullable = false)
    @Type(type = "uuid-char")
    private UUID authorId;

    @Lob
    private String content;

    @Lob
    private String mediaUrls;

    private String location;

    @Lob
    private String topics;

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Feed() {
    }

    public Feed(UUID feedId, UUID authorId, String content) {
        this.feedId = feedId;
        this.authorId = authorId;
        this.content = content;
        this.visibility = Visibility.PUBLIC;
        this.createdAt = Instant.now();
    }

    public UUID getFeedId() {
        return feedId;
    }

    public void setFeedId(UUID feedId) {
        this.feedId = feedId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(String mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
