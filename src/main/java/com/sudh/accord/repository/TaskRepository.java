package com.sudh.accord.repository;

import com.sudh.accord.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findAllByUserId(UUID userId);

    // Denominator for completionRate. Tasks created before the createdAt
    // column existed will have a null createdAt and are correctly excluded
    // by BETWEEN (SQL treats comparisons against NULL as unknown, not true).
    long countByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);
}
