package com.tarotalk.world.repo;

import com.tarotalk.world.domain.StoryArc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoryArcRepository extends JpaRepository<StoryArc, UUID> {
    List<StoryArc> findByWorldIdOrderByUpdatedAtDesc(UUID worldId);
}
