package com.sudh.accord.controller;

import com.sudh.accord.dto.CreateTaskRequest;
import com.sudh.accord.dto.TaskResponse;
import com.sudh.accord.entity.Task;
import com.sudh.accord.enums.TaskType;
import com.sudh.accord.service.TaskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    public TaskResponse createTask(@RequestBody CreateTaskRequest request, @AuthenticationPrincipal String userId) {
        Task task = new Task(
                null,
                request.title(),
                request.description(),
                request.value(),
                TaskType.valueOf(request.type()),
                false,
                request.dueDate() != null ? LocalDate.parse(request.dueDate()) : null,
                null,
                null
        );
        return toResponse(taskService.createTask(task, UUID.fromString(userId)));
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable UUID id, @AuthenticationPrincipal String userId) {
        taskService.deleteTask(id, UUID.fromString(userId));
    }

    @PatchMapping("/{id}/complete")
    public TaskResponse completeTask(@PathVariable UUID id, @AuthenticationPrincipal String userId) {
        return toResponse(taskService.completeTask(id, UUID.fromString(userId)));
    }

    // ── mapping ──────────────────────────────────────────────────────────────

    private TaskResponse toResponse(Task task) {
        // For recurring tasks, isCompleted reflects the current cycle, not the raw
        // (possibly stale, from a prior cycle) DB flag.
        boolean effectiveCompleted = task.getType() == TaskType.ONE_OFF
                ? task.getCompleted()
                : TaskService.isCompletedThisCycle(task);
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getValue(),
                task.getType(),
                effectiveCompleted,
                task.getDueDate() != null ? task.getDueDate().toString() : null,
                task.getLastCompletedAt() != null ? task.getLastCompletedAt().toString() : null,
                task.getUser().getId()
        );
    }
}