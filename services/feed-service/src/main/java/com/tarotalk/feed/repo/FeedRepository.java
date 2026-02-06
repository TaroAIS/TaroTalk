package com.tarotalk.feed.repo;

import com.tarotalk.feed.domain.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface FeedRepository extends JpaRepository<Feed, UUID> {
    Page<Feed> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Feed> findByCreatedAtBeforeOrderByCreatedAtDesc(java.time.Instant createdAt, Pageable pageable);
    Page<Feed> findByAuthorIdInOrderByCreatedAtDesc(List<UUID> authorIds, Pageable pageable);
    Page<Feed> findByAuthorIdInAndCreatedAtBeforeOrderByCreatedAtDesc(List<UUID> authorIds, java.time.Instant createdAt, Pageable pageable);
}
