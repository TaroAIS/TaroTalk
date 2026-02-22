package com.tarotalk.scheduler;

import com.tarotalk.scheduler.api.SchedulerController;
import com.tarotalk.scheduler.service.TaskSchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = SchedulerServiceApplication.class)
public class SchedulerContextLoadTest {
    @Autowired
    private SchedulerController schedulerController;

    @Autowired
    private TaskSchedulerService taskSchedulerService;

    @Test
    void contextLoads() {
        assertNotNull(schedulerController);
        assertNotNull(taskSchedulerService);
    }
}

