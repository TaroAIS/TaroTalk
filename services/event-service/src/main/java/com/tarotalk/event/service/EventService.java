package com.tarotalk.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.common.exception.ApiException;
import com.tarotalk.event.api.InternalEventRequest;
import com.tarotalk.event.domain.EventLog;
import com.tarotalk.event.repo.EventLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {
    private final EventLogRepository eventLogRepository;
    private final ObjectMapper objectMapper;

    public EventService(EventLogRepository eventLogRepository, ObjectMapper objectMapper) {
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = objectMapper;
    }

    public EventLog append(InternalEventRequest request) {
        String sourceService = clean(request.getSourceService());
        String idempotencyKey = clean(request.getIdempotencyKey());
        if (sourceService != null && idempotencyKey != null) {
            java.util.Optional<EventLog> existing = eventLogRepository
                    .findFirstBySourceServiceAndIdempotencyKey(sourceService, idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        EventLog event = new EventLog(UUID.randomUUID(), request.getEventType().trim());
        event.setActorId(clean(request.getActorId()));
        event.setTargetId(clean(request.getTargetId()));
        event.setEntityType(clean(request.getEntityType()));
        event.setEntityId(clean(request.getEntityId()));
        event.setIdempotencyKey(idempotencyKey);
        event.setTraceId(clean(request.getTraceId()));
        event.setSourceService(sourceService);
        event.setPayloadJson(toPayloadJson(request.getPayload()));
        event.setCreatedAt(request.getCreatedAt() == null ? Instant.now() : request.getCreatedAt());
        return eventLogRepository.save(event);
    }

    public List<EventLog> query(String entityType,
                                String entityId,
                                String actorId,
                                String targetId,
                                Instant from,
                                Instant to,
                                Integer limit) {
        Specification<EventLog> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(entityType)) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
            }
            if (hasText(entityId)) {
                predicates.add(cb.equal(root.get("entityId"), entityId.trim()));
            }
            if (hasText(actorId)) {
                predicates.add(cb.equal(root.get("actorId"), actorId.trim()));
            }
            if (hasText(targetId)) {
                predicates.add(cb.equal(root.get("targetId"), targetId.trim()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return eventLogRepository.findAll(
                specification,
                PageRequest.of(0, resolveLimit(limit), Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }

    public List<EventLog> replay(String traceId) {
        String value = clean(traceId);
        if (value == null) {
            throw new ApiException("VALIDATION_ERROR", "traceId is required");
        }
        return eventLogRepository.findByTraceIdOrderByCreatedAtAsc(value);
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 200;
        }
        return Math.min(limit, 1000);
    }

    private String toPayloadJson(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String) {
            return (String) payload;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new ApiException("SERIALIZATION_ERROR", "payload serialization failed");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
