package com.tarotalk.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.scheduler.api.TaskRequest;
import com.tarotalk.scheduler.api.TaskResponse;
import com.tarotalk.scheduler.domain.ScheduledTask;
import com.tarotalk.scheduler.domain.TaskStatus;
import com.tarotalk.scheduler.repo.ScheduledTaskRepository;
import com.tarotalk.scheduler.service.TaskSchedulerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TaskSchedulerServiceTest {
    @Test
    void registerReturnsExistingTaskWhenIdempotencyKeyExists() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ScheduledTaskRepository repository = mock(ScheduledTaskRepository.class);
        TaskSchedulerService service = new TaskSchedulerService(restTemplate, repository, new ObjectMapper(), "http://orchestrator", "", "direct", 60000L);

        ScheduledTask existing = new ScheduledTask(UUID.randomUUID(), "SIMULATE");
        existing.setIdempotencyKey("dup-key");
        existing.setStatus(TaskStatus.SUCCEEDED);
        existing.setNextRunAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(repository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        TaskRequest request = new TaskRequest();
        request.setTaskType("SIMULATE");
        request.setIdempotencyKey("dup-key");

        TaskResponse response = service.register(request);
        assertEquals(existing.getTaskId(), response.getTaskId());
        verify(repository, never()).save(any(ScheduledTask.class));
    }

    @Test
    void runTasksMarksSucceededWhenSimulateCallSucceeds() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ScheduledTaskRepository repository = mock(ScheduledTaskRepository.class);
        TaskSchedulerService service = new TaskSchedulerService(restTemplate, repository, new ObjectMapper(), "http://orchestrator", "", "direct", 60000L);

        ScheduledTask task = new ScheduledTask(UUID.randomUUID(), "SIMULATE");
        task.setTargetId(UUID.randomUUID().toString());
        task.setEnabled(true);
        task.setStatus(TaskStatus.PENDING);
        task.setNextRunAt(Instant.now().minusSeconds(1));
        task.setUpdatedAt(Instant.now());

        when(repository.findByEnabledTrueOrderByNextRunAtAsc()).thenReturn(Collections.singletonList(task));
        when(repository.save(any(ScheduledTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.runTasks();

        verify(restTemplate, times(1)).postForObject(eq("http://orchestrator/api/v2/a2a/simulate"), any(), eq(String.class));
        ArgumentCaptor<ScheduledTask> captor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(repository, atLeast(2)).save(captor.capture());
        ScheduledTask finalState = captor.getValue();
        assertEquals(TaskStatus.SUCCEEDED, finalState.getStatus());
        assertNotNull(finalState.getLastRunAt());
        assertNotNull(finalState.getNextRunAt());
    }

    @Test
    void runTasksDisablesTaskAfterMaxRetryReached() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ScheduledTaskRepository repository = mock(ScheduledTaskRepository.class);
        TaskSchedulerService service = new TaskSchedulerService(
                restTemplate,
                repository,
                new ObjectMapper(),
                "http://orchestrator",
                "",
                "direct",
                60000L,
                2,
                1000L
        );

        ScheduledTask task = new ScheduledTask(UUID.randomUUID(), "SIMULATE");
        task.setTargetId(UUID.randomUUID().toString());
        task.setEnabled(true);
        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(1);
        task.setNextRunAt(Instant.now().minusSeconds(1));
        task.setUpdatedAt(Instant.now());

        when(repository.findByEnabledTrueOrderByNextRunAtAsc()).thenReturn(Collections.singletonList(task));
        when(repository.save(any(ScheduledTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.postForObject(eq("http://orchestrator/api/v2/a2a/simulate"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("down"));
        when(restTemplate.postForObject(eq("http://orchestrator/a2a/simulate"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("down"));

        service.runTasks();

        ArgumentCaptor<ScheduledTask> captor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(repository, atLeast(2)).save(captor.capture());
        ScheduledTask finalState = captor.getValue();
        assertEquals(TaskStatus.FAILED, finalState.getStatus());
        assertEquals(false, finalState.isEnabled());
    }
}
