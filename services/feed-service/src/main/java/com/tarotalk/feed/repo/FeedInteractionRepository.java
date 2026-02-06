package com.tarotalk.feed.repo;

import com.tarotalk.feed.domain.FeedInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedInteractionRepository extends JpaRepository<FeedInteraction, UUID> {
    List<FeedInteraction> findByFeedId(UUID feedId);
    List<FeedInteraction> findByFeedIdIn(List<UUID> feedIds);
}
