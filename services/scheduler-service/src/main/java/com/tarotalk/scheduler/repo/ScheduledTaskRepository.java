package com.tarotalk.scheduler.repo;

import com.tarotalk.scheduler.domain.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, UUID> {
    Optional<ScheduledTask> findByIdempotencyKey(String idempotencyKey);
    List<ScheduledTask> findByEnabledTrue();
    List<ScheduledTask> findByEnabledTrueOrderByNextRunAtAsc();
}
