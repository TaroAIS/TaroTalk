package com.tarotalk.scheduler.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.scheduler.service.TaskSchedulerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/scheduler/tasks")
@Validated
public class SchedulerController {
    private final TaskSchedulerService schedulerService;

    public SchedulerController(TaskSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody TaskRequest request) {
        schedulerService.register(request);
        return ApiResponse.ok(null);
    }

    @GetMapping
    public ApiResponse<List<TaskRequest>> list() {
        return ApiResponse.ok(schedulerService.list());
    }
}
