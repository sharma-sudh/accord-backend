package com.sudh.accord.service;

import com.sudh.accord.entity.Transaction;
import com.sudh.accord.enums.TransactionType;
import com.sudh.accord.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createTransaction(Transaction transaction){
        return transactionRepository.save(transaction);
    }

    public BigDecimal getBudget(UUID id){
        BigDecimal earned = transactionRepository.getSumByType(id, TransactionType.TASK_COMPLETED);
        BigDecimal spent = transactionRepository.getSumByType(id, TransactionType.PAYMENT_MADE);

        BigDecimal totalEarned = earned == null ? BigDecimal.ZERO : earned;
        BigDecimal totalSpent = spent == null ? BigDecimal.ZERO : spent;

        return totalEarned.subtract(totalSpent);
    }
}
