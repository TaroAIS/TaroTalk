package com.tarotalk.world.repo;

import com.tarotalk.world.domain.AgentGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentGoalRepository extends JpaRepository<AgentGoal, UUID> {
    List<AgentGoal> findByWorldId(UUID worldId);
}
