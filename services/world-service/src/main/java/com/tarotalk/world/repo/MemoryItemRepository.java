package com.tarotalk.world.repo;

import com.tarotalk.world.domain.MemoryItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryItemRepository extends JpaRepository<MemoryItem, UUID> {
    Optional<MemoryItem> findFirstByWorldIdAndOwnerIdAndSourceEventIdAndSummaryHash(
            UUID worldId,
            String ownerId,
            String sourceEventId,
            String summaryHash
    );

    List<MemoryItem> findByWorldIdOrderByUpdatedAtDesc(UUID worldId, Pageable pageable);

    List<MemoryItem> findByWorldIdAndOwnerIdOrderByUpdatedAtDesc(UUID worldId, String ownerId, Pageable pageable);
}
