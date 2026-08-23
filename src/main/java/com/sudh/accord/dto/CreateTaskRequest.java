package com.sudh.accord.dto;

import java.math.BigDecimal;

public record CreateTaskRequest(
        String title,
        String description,
        BigDecimal value,
        String type,
        String dueDate
) {}