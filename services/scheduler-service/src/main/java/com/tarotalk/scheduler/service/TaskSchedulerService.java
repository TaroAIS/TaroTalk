package com.tarotalk.scheduler.service;

import com.tarotalk.scheduler.api.TaskRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TaskSchedulerService {
    private final RestTemplate restTemplate;
    private final String orchestratorUrl;
    private final List<TaskRequest> tasks = new CopyOnWriteArrayList<>();

    public TaskSchedulerService(RestTemplate restTemplate,
                                @Value("${integrations.orchestrator.base-url}") String orchestratorUrl) {
        this.restTemplate = restTemplate;
        this.orchestratorUrl = orchestratorUrl;
    }

    public void register(TaskRequest request) {
        tasks.add(request);
    }

    public List<TaskRequest> list() {
        return tasks;
    }

    @Scheduled(fixedDelayString = "${scheduler.loop-delay-ms:60000}")
    public void runTasks() {
        for (TaskRequest task : tasks) {
            triggerSimulation(task);
        }
    }

    private void triggerSimulation(TaskRequest task) {
        if (orchestratorUrl == null || orchestratorUrl.trim().isEmpty()) {
            return;
        }
        try {
            restTemplate.postForObject(orchestratorUrl + "/a2a/simulate", task, String.class);
        } catch (Exception ex) {
            // swallow for now
        }
    }
}
