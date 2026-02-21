package com.tarotalk.world.repo;

import com.tarotalk.world.domain.WorldCausalEdge;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorldCausalEdgeRepository extends JpaRepository<WorldCausalEdge, UUID> {
    Optional<WorldCausalEdge> findFirstByWorldIdAndCauseEventIdAndEffectEventIdAndRelationType(
            UUID worldId,
            String causeEventId,
            String effectEventId,
            String relationType
    );

    List<WorldCausalEdge> findByWorldIdAndCauseEventIdOrderByCreatedAtAsc(UUID worldId, String causeEventId);

    List<WorldCausalEdge> findByWorldIdOrderByCreatedAtDesc(UUID worldId, Pageable pageable);
}
