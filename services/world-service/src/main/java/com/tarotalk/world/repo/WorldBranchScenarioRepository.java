package com.tarotalk.world.repo;

import com.tarotalk.world.domain.WorldBranchScenario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorldBranchScenarioRepository extends JpaRepository<WorldBranchScenario, UUID> {
    List<WorldBranchScenario> findByWorldIdOrderByCreatedAtDesc(UUID worldId, Pageable pageable);
}
