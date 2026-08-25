package com.sudh.accord.dto;

import com.sudh.accord.enums.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        BigDecimal amount,
        TransactionType type,
        String merchantName,
        String createdAt
) {}