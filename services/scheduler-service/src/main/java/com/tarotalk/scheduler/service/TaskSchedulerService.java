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
    private final String aiServiceUrl;
    private final String chatServiceUrl;
    private final String feedServiceUrl;
    private final List<TaskRequest> tasks = new CopyOnWriteArrayList<>();

    public TaskSchedulerService(RestTemplate restTemplate,
                                @Value("${integrations.ai-service.base-url}") String aiServiceUrl,
                                @Value("${integrations.chat-service.base-url}") String chatServiceUrl,
                                @Value("${integrations.feed-service.base-url}") String feedServiceUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
        this.chatServiceUrl = chatServiceUrl;
        this.feedServiceUrl = feedServiceUrl;
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
            if ("CHAT".equalsIgnoreCase(task.getTaskType())) {
                triggerChat(task);
            } else if ("FEED".equalsIgnoreCase(task.getTaskType())) {
                triggerFeed(task);
            }
        }
    }

    private void triggerChat(TaskRequest task) {
        if (aiServiceUrl == null || chatServiceUrl == null) {
            return;
        }
        try {
            String aiPayload = restTemplate.postForObject(aiServiceUrl + "/api/ai/reply", task, String.class);
            restTemplate.postForObject(chatServiceUrl + "/api/conversations/" + task.getTargetId() + "/messages", task, String.class);
        } catch (Exception ex) {
            // swallow for now
        }
    }

    private void triggerFeed(TaskRequest task) {
        if (aiServiceUrl == null || feedServiceUrl == null) {
            return;
        }
        try {
            restTemplate.postForObject(aiServiceUrl + "/api/ai/feed", task, String.class);
            restTemplate.postForObject(feedServiceUrl + "/api/feeds", task, String.class);
        } catch (Exception ex) {
            // swallow for now
        }
    }
}
