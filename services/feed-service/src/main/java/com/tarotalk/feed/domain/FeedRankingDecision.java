package com.tarotalk.feed.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "feed_ranking_decision",
        indexes = {
                @Index(name = "idx_ranking_viewer_created", columnList = "viewer_id,created_at"),
                @Index(name = "idx_ranking_feed_created", columnList = "feed_id,created_at"),
                @Index(name = "idx_ranking_trace_created", columnList = "trace_id,created_at")
        }
)
public class FeedRankingDecision {
    @Id
    @Type(type = "uuid-char")
    @Column(name = "decision_id", nullable = false, updatable = false)
    private UUID decisionId;

    @Type(type = "uuid-char")
    @Column(name = "viewer_id", nullable = false)
    private UUID viewerId;

    @Type(type = "uuid-char")
    @Column(name = "feed_id", nullable = false)
    private UUID feedId;

    @Column(name = "policy", nullable = false, length = 64)
    private String policy;

    @Lob
    @Column(name = "context_json")
    private String contextJson;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "chosen", nullable = false)
    private Boolean chosen;

    @Column(name = "reward", nullable = false)
    private Double reward;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FeedRankingDecision() {
    }

    public FeedRankingDecision(UUID decisionId, UUID viewerId, UUID feedId, String policy, Double score) {
        this.decisionId = decisionId;
        this.viewerId = viewerId;
        this.feedId = feedId;
        this.policy = policy;
        this.score = score;
        this.chosen = Boolean.FALSE;
        this.reward = 0.0;
        this.createdAt = Instant.now();
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(UUID decisionId) {
        this.decisionId = decisionId;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public void setViewerId(UUID viewerId) {
        this.viewerId = viewerId;
    }

    public UUID getFeedId() {
        return feedId;
    }

    public void setFeedId(UUID feedId) {
        this.feedId = feedId;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Boolean getChosen() {
        return chosen;
    }

    public void setChosen(Boolean chosen) {
        this.chosen = chosen;
    }

    public Double getReward() {
        return reward;
    }

    public void setReward(Double reward) {
        this.reward = reward;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
