package com.sudh.accord.dto;

import com.sudh.accord.enums.TaskType;

import java.math.BigDecimal;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        BigDecimal value,
        TaskType type,
        boolean isCompleted,
        String dueDate,
        UUID userId
) {}