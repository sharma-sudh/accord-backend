package com.sudh.accord.service;

import com.sudh.accord.dto.CreateTaskRequest;
import com.sudh.accord.entity.Task;
import com.sudh.accord.entity.Transaction;
import com.sudh.accord.entity.User;
import com.sudh.accord.enums.TaskType;
import com.sudh.accord.enums.TransactionType;
import com.sudh.accord.repository.TaskRepository;
import com.sudh.accord.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
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
}