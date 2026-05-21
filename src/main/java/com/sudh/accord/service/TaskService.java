package com.sudh.accord.service;

import com.sudh.accord.entity.Task;
import com.sudh.accord.entity.Transaction;
import com.sudh.accord.enums.TransactionType;
import com.sudh.accord.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TransactionService transactionService;

    public TaskService(TaskRepository taskRepository, TransactionService transactionService) {
        this.taskRepository = taskRepository;
        this.transactionService = transactionService;
    }

    public List<Task> getAllTasks(){
        return taskRepository.findAll();
    }

    public Task createTask(Task task){
        return taskRepository.save(task);
    }

    public void deleteTask(UUID id){
        taskRepository.deleteById(id);
    }

    public Task completeTask(UUID id){
        Task task = taskRepository.findById(id).orElseThrow();
        task.setCompleted(true);
        Transaction transaction = new Transaction(null,
                task.getValue(),
                task.getUser(),
                task,
                TransactionType.TASK_COMPLETED,
                LocalDateTime.now(),
                LocalDateTime.now());
        transactionService.createTransaction(transaction);
        return taskRepository.save(task);
    }
}
