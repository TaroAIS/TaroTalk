package com.tarotalk.scheduler.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.scheduler.api.TaskRequest;
import com.tarotalk.scheduler.api.TaskResponse;
import com.tarotalk.scheduler.domain.ScheduledTask;
import com.tarotalk.scheduler.domain.TaskStatus;
import com.tarotalk.scheduler.repo.ScheduledTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskSchedulerService {
    private final RestTemplate restTemplate;
    private final ScheduledTaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final String orchestratorUrl;
    private final String temporalServiceUrl;
    private final String workflowMode;
    private final long defaultRunIntervalMs;
    private final int maxRetries;
    private final long retryBackoffMs;

    public TaskSchedulerService(RestTemplate restTemplate,
                                ScheduledTaskRepository taskRepository,
                                ObjectMapper objectMapper,
                                String orchestratorUrl,
                                String temporalServiceUrl,
                                String workflowMode,
                                long defaultRunIntervalMs) {
        this(
                restTemplate,
                taskRepository,
                objectMapper,
                orchestratorUrl,
                temporalServiceUrl,
                workflowMode,
                defaultRunIntervalMs,
                5,
                30000L
        );
    }

    public TaskSchedulerService(RestTemplate restTemplate,
                                ScheduledTaskRepository taskRepository,
                                ObjectMapper objectMapper,
                                @Value("${integrations.orchestrator.base-url}") String orchestratorUrl,
                                @Value("${integrations.temporal-service.base-url:}") String temporalServiceUrl,
                                @Value("${scheduler.workflow.mode:direct}") String workflowMode,
                                @Value("${scheduler.default-run-interval-ms:60000}") long defaultRunIntervalMs,
                                @Value("${scheduler.max-retries:5}") int maxRetries,
                                @Value("${scheduler.retry-backoff-ms:30000}") long retryBackoffMs) {
        this.restTemplate = restTemplate;
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
        this.orchestratorUrl = orchestratorUrl;
        this.temporalServiceUrl = temporalServiceUrl;
        this.workflowMode = workflowMode;
        this.defaultRunIntervalMs = defaultRunIntervalMs;
        this.maxRetries = Math.max(1, maxRetries);
        this.retryBackoffMs = Math.max(1000L, retryBackoffMs);
    }

    public TaskResponse register(TaskRequest request) {
        String idempotencyKey = clean(request.getIdempotencyKey());
        if (idempotencyKey != null) {
            Optional<ScheduledTask> existing = taskRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        UUID taskId = request.getTaskId() == null ? UUID.randomUUID() : request.getTaskId();
        ScheduledTask task = new ScheduledTask(taskId, request.getTaskType().trim());
        task.setTargetId(clean(request.getTargetId()));
        task.setPayload(clean(request.getPayload()));
        task.setWorldId(clean(request.getWorldId()));
        task.setTriggerType(clean(request.getTriggerType()));
        task.setObjective(clean(request.getObjective()));
        task.setActorsJson(writeActors(request.getActors()));
        task.setPriority(request.getPriority());
        task.setIdempotencyKey(idempotencyKey == null ? "task:" + taskId : idempotencyKey);
        task.setScheduleExpr(clean(request.getScheduleExpr()));
        task.setEnabled(request.getEnabled() == null || request.getEnabled());
        task.setStatus(resolveStatus(request.getStatus()));
        task.setRetryCount(0);
        task.setLastError(null);
        task.setLastRunAt(null);
        task.setNextRunAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        return toResponse(taskRepository.save(task));
    }

    public List<TaskResponse> list() {
        List<ScheduledTask> rows = taskRepository.findAll();
        rows.sort(Comparator.comparing(ScheduledTask::getCreatedAt).reversed());
        List<TaskResponse> response = new ArrayList<>();
        for (ScheduledTask row : rows) {
            response.add(toResponse(row));
        }
        return response;
    }

    @Scheduled(fixedDelayString = "${scheduler.loop-delay-ms:60000}")
    public void runTasks() {
        Instant now = Instant.now();
        for (ScheduledTask task : taskRepository.findByEnabledTrueOrderByNextRunAtAsc()) {
            if (task.getNextRunAt() != null && task.getNextRunAt().isAfter(now)) {
                continue;
            }
            try {
                executeOnce(task, now);
            } catch (Exception ex) {
                task.setStatus(TaskStatus.FAILED);
                task.setRetryCount((task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1);
                task.setLastError(ex.getMessage());
                task.setUpdatedAt(Instant.now());
                task.setNextRunAt(now.plusMillis(computeRetryDelayMs(task.getRetryCount())));
                if (task.getRetryCount() >= maxRetries) {
                    task.setEnabled(false);
                }
                taskRepository.save(task);
            }
        }
    }

    private void executeOnce(ScheduledTask task, Instant startedAt) {
        task.setStatus(TaskStatus.RUNNING);
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);

        boolean success = triggerSimulation(task);
        task.setLastRunAt(startedAt);
        task.setUpdatedAt(Instant.now());
        if (success) {
            task.setStatus(TaskStatus.SUCCEEDED);
            task.setLastError(null);
            task.setRetryCount(0);
            task.setNextRunAt(startedAt.plusMillis(Math.max(1000L, defaultRunIntervalMs)));
        } else {
            task.setStatus(TaskStatus.FAILED);
            task.setRetryCount((task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1);
            if (task.getLastError() == null || task.getLastError().trim().isEmpty()) {
                task.setLastError("simulate invoke failed");
            }
            if (task.getRetryCount() >= maxRetries) {
                task.setEnabled(false);
                task.setNextRunAt(null);
            } else {
                task.setNextRunAt(startedAt.plusMillis(computeRetryDelayMs(task.getRetryCount())));
            }
        }
        taskRepository.save(task);
    }

    private boolean triggerSimulation(ScheduledTask task) {
        if ("temporal".equalsIgnoreCase(workflowMode) && hasText(temporalServiceUrl)) {
            boolean submitted = submitTemporalWorkflow(task);
            if (submitted) {
                return true;
            }
        }
        if (orchestratorUrl == null || orchestratorUrl.trim().isEmpty()) {
            task.setLastError("orchestrator url missing");
            return false;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", task.getTargetId());
            payload.put("world_id", hasText(task.getWorldId()) ? task.getWorldId() : task.getTargetId());
            payload.put("trigger_type", hasText(task.getTriggerType()) ? task.getTriggerType() : task.getTaskType());
            payload.put("objective", hasText(task.getObjective()) ? task.getObjective() : task.getPayload());
            payload.put("actors", readActors(task.getActorsJson()));
            payload.put("priority", task.getPriority());
            payload.put("idempotency_key", task.getIdempotencyKey());
            restTemplate.postForObject(orchestratorUrl + "/api/v2/a2a/simulate", payload, String.class);
            return true;
        } catch (Exception ex) {
            task.setLastError(ex.getMessage());
            try {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("user_id", task.getTargetId());
                fallback.put("objective", hasText(task.getObjective()) ? task.getObjective() : task.getPayload());
                restTemplate.postForObject(orchestratorUrl + "/a2a/simulate", fallback, String.class);
                return true;
            } catch (Exception fallbackEx) {
                task.setLastError(fallbackEx.getMessage());
                return false;
            }
        }
    }

    private TaskResponse toResponse(ScheduledTask task) {
        return TaskResponse.from(task, readActors(task.getActorsJson()));
    }

    private boolean submitTemporalWorkflow(ScheduledTask task) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("task_id", task.getTaskId().toString());
            payload.put("workflow_id", "workflow-" + task.getTaskId());
            payload.put("workflow_type", "SOCIAL_SIMULATE_WORKFLOW");
            payload.put("world_id", task.getWorldId());
            payload.put("objective", task.getObjective());
            payload.put("actors", readActors(task.getActorsJson()));
            payload.put("idempotency_key", task.getIdempotencyKey());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(
                    temporalServiceUrl + "/api/v1/workflows/simulate",
                    new HttpEntity<>(payload, headers),
                    Map.class
            );
            return true;
        } catch (Exception ex) {
            task.setLastError(ex.getMessage());
            return false;
        }
    }

    private String writeActors(List<String> actors) {
        try {
            return objectMapper.writeValueAsString(actors == null ? new ArrayList<>() : actors);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> readActors(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<String> values = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            return values == null ? new ArrayList<>() : values;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private TaskStatus resolveStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return TaskStatus.PENDING;
        }
        try {
            return TaskStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            return TaskStatus.PENDING;
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private long computeRetryDelayMs(Integer retryCount) {
        int attempts = retryCount == null ? 1 : Math.max(1, retryCount);
        long multiplier = 1L << Math.min(6, attempts - 1);
        long delay = retryBackoffMs * multiplier;
        long cap = Math.max(defaultRunIntervalMs * 4, retryBackoffMs);
        return Math.min(delay, cap);
    }
}
