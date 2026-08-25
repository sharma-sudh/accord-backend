package com.sudh.accord.service;

import com.sudh.accord.entity.Transaction;
import com.sudh.accord.entity.User;
import com.sudh.accord.enums.TransactionType;
import com.sudh.accord.repository.TransactionRepository;
import com.sudh.accord.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // Package-private: only service-layer code (e.g. TaskService.completeTask,
    // the future payment-deduction flow) may create transactions. No controller
    // should call this directly with a client-supplied Transaction.
    Transaction createTransaction(Transaction transaction){
        return transactionRepository.save(transaction);
    }

    // The only client-facing way to create a transaction. Type is hardcoded to
    // PAYMENT_MADE and user comes from the authenticated principal — never
    // from client input.
    public Transaction createPayment(UUID userId, BigDecimal amount, String merchantName) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (merchantName == null || merchantName.isBlank()) {
            throw new IllegalArgumentException("merchantName is required");
        }

        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction(
                null,
                amount,
                user,
                null,
                TransactionType.PAYMENT_MADE,
                merchantName,
                now,
                now
        );
        return createTransaction(transaction);
    }

    public BigDecimal getBudget(UUID id){
        BigDecimal earned = transactionRepository.getSumByType(id, TransactionType.TASK_COMPLETED);
        BigDecimal spent = transactionRepository.getSumByType(id, TransactionType.PAYMENT_MADE);

        BigDecimal totalEarned = earned == null ? BigDecimal.ZERO : earned;
        BigDecimal totalSpent = spent == null ? BigDecimal.ZERO : spent;

        return totalEarned.subtract(totalSpent);
    }
}