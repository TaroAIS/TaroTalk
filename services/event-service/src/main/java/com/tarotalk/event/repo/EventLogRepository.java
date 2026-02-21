package com.tarotalk.event.repo;

import com.tarotalk.event.domain.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventLogRepository extends JpaRepository<EventLog, UUID>, JpaSpecificationExecutor<EventLog> {
    List<EventLog> findByTraceIdOrderByCreatedAtAsc(String traceId);
    Optional<EventLog> findFirstBySourceServiceAndIdempotencyKey(String sourceService, String idempotencyKey);
}
