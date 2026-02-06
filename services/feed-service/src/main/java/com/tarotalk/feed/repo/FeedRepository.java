package com.tarotalk.feed.repo;

import com.tarotalk.feed.domain.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedRepository extends JpaRepository<Feed, UUID> {
    List<Feed> findTop20ByOrderByCreatedAtDesc();
    List<Feed> findTop20ByAuthorIdInOrderByCreatedAtDesc(List<UUID> authorIds);
}
