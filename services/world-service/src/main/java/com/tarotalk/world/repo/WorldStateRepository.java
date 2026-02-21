package com.tarotalk.world.repo;

import com.tarotalk.world.domain.WorldState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorldStateRepository extends JpaRepository<WorldState, UUID> {
}
