package com.tarotalk.world.repo;

import com.tarotalk.world.domain.WorldEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorldEventRepository extends JpaRepository<WorldEvent, UUID> {
    List<WorldEvent> findByWorldIdOrderByCreatedAtDesc(UUID worldId, Pageable pageable);
}
