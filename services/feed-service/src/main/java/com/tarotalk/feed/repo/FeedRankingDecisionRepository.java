package com.tarotalk.feed.repo;

import com.tarotalk.feed.domain.FeedRankingDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeedRankingDecisionRepository extends JpaRepository<FeedRankingDecision, UUID> {
    Optional<FeedRankingDecision> findFirstByViewerIdAndFeedIdOrderByCreatedAtDesc(UUID viewerId, UUID feedId);
}
