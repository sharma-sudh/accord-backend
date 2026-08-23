package com.sudh.accord.controller;

import com.sudh.accord.dto.CreateTaskRequest;
import com.sudh.accord.dto.TaskResponse;
import com.sudh.accord.entity.Task;
import com.sudh.accord.service.TaskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> getAllTasks(@AuthenticationPrincipal String userId) {
        return taskService.getAllTasks(UUID.fromString(userId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public Task createTask(@RequestBody Task task, @AuthenticationPrincipal String userId){
        return taskService.createTask(task, UUID.fromString(userId));
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
    }

    @PatchMapping("/{id}/complete")
    public TaskResponse completeTask(@PathVariable UUID id) {
        return toResponse(taskService.completeTask(id));
    }

    // ── mapping ──────────────────────────────────────────────────────────────

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getValue(),
                task.getType(),
                task.getCompleted(),
                task.getDueDate() != null ? task.getDueDate().toString() : null,
                task.getUser().getId()
        );
    }
}