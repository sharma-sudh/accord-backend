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
        return taskRepository.findAllByUserId(userId);
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