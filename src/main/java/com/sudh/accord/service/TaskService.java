package com.sudh.accord.service;

import com.sudh.accord.dto.CreateTaskRequest;
import com.sudh.accord.dto.TaskSyncRequest;
import com.sudh.accord.entity.Task;
import com.sudh.accord.entity.Transaction;
import com.sudh.accord.entity.User;
import com.sudh.accord.enums.SyncStatus;
import com.sudh.accord.enums.TaskType;
import com.sudh.accord.enums.TransactionType;
import com.sudh.accord.repository.TaskRepository;
import com.sudh.accord.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public TaskService(TaskRepository taskRepository, TransactionService transactionService, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    public List<Task> getAllTasks(UUID userId) {
        return taskRepository.findAllByUserId(userId).stream()
                .filter(task -> task.getType() == TaskType.ONE_OFF || !isCompletedThisCycle(task))
                .toList();
    }

    /**
     * Whether a recurring task has already been completed within its current cycle
     * (today for DAILY, this ISO week for WEEKLY, this calendar month for MONTHLY).
     * Not meaningful for ONE_OFF tasks, which rely on isCompleted directly.
     */
    public static boolean isCompletedThisCycle(Task task) {
        LocalDateTime lastCompletedAt = task.getLastCompletedAt();
        if (lastCompletedAt == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return switch (task.getType()) {
            case DAILY -> lastCompletedAt.toLocalDate().isEqual(now.toLocalDate());
            case WEEKLY -> {
                WeekFields weekFields = WeekFields.ISO;
                yield lastCompletedAt.get(weekFields.weekBasedYear()) == now.get(weekFields.weekBasedYear())
                        && lastCompletedAt.get(weekFields.weekOfWeekBasedYear()) == now.get(weekFields.weekOfWeekBasedYear());
            }
            case MONTHLY -> lastCompletedAt.getYear() == now.getYear()
                    && lastCompletedAt.getMonthValue() == now.getMonthValue();
            case ONE_OFF -> false;
        };
    }

    public Task createTask(Task task, UUID userId){
        User user = userRepository.findById(userId).orElseThrow();
        task.setUser(user);
        return taskRepository.save(task);
    }

    public void deleteTask(UUID id, UUID userId) {
        Task task = taskRepository.findById(id).orElseThrow();
        if (!task.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Not authorized to delete this task");
        }
        taskRepository.deleteById(id);
    }

    public Task completeTask(UUID id, UUID userId) {
        Task task = taskRepository.findById(id).orElseThrow();
        if (!task.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Not authorized to complete this task");
        }
        task.setCompleted(true);
        task.setLastCompletedAt(LocalDateTime.now());
        task.setVersion(task.getVersion() + 1);
        task.setLastChangedFields("isCompleted,lastCompletedAt");
        Transaction transaction = new Transaction(
                null,
                task.getValue(),
                task.getUser(),
                task,
                TransactionType.TASK_COMPLETED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        transactionService.createTransaction(transaction);
        return taskRepository.save(task);
    }

    /**
     * Result of a version-vector sync attempt. `task` holds the updated task
     * for APPLIED/MERGED, or the untouched current server copy for CONFLICT.
     * `conflictingFields` is only populated for CONFLICT.
     */
    public record TaskSyncResult(SyncStatus status, Task task, List<String> conflictingFields) {}

    public TaskSyncResult syncTask(UUID id, UUID userId, TaskSyncRequest request) {
        Task task = taskRepository.findById(id).orElseThrow();
        if (!task.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Not authorized to sync this task");
        }

        Set<String> incomingFields = request.changes().keySet();

        if (request.baseVersion() == task.getVersion()) {
            applyChanges(task, request.changes());
            task.setVersion(task.getVersion() + 1);
            task.setLastChangedFields(String.join(",", incomingFields));
            return new TaskSyncResult(SyncStatus.APPLIED, taskRepository.save(task), null);
        }

        // baseVersion is stale: something else bumped the version since the
        // client last saw this task. Compare the fields that update touched
        // against the fields the client wants to touch now.
        Set<String> serverChangedFields = parseChangedFields(task.getLastChangedFields());
        Set<String> overlappingFields = new HashSet<>(serverChangedFields);
        overlappingFields.retainAll(incomingFields);

        if (overlappingFields.isEmpty()) {
            applyChanges(task, request.changes());
            task.setVersion(task.getVersion() + 1);
            Set<String> combinedFields = new HashSet<>(serverChangedFields);
            combinedFields.addAll(incomingFields);
            task.setLastChangedFields(String.join(",", combinedFields));
            return new TaskSyncResult(SyncStatus.MERGED, taskRepository.save(task), null);
        }

        return new TaskSyncResult(SyncStatus.CONFLICT, task, List.copyOf(overlappingFields));
    }

    private static Set<String> parseChangedFields(String stored) {
        if (stored == null || stored.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(stored.split(",")));
    }

    // Only isCompleted/lastCompletedAt exist today (task completion is the
    // only thing that produces a PENDING_UPDATE row on Android). Unknown keys
    // are ignored rather than rejected, so an older server doesn't hard-fail
    // against a newer client that starts sending additional fields.
    private static void applyChanges(Task task, Map<String, Object> changes) {
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            switch (entry.getKey()) {
                case "isCompleted" -> task.setCompleted((Boolean) entry.getValue());
                case "lastCompletedAt" -> task.setLastCompletedAt(
                        entry.getValue() != null ? LocalDateTime.parse((String) entry.getValue()) : null);
                default -> { /* unknown field — ignore for forward-compat */ }
            }
        }
    }
}