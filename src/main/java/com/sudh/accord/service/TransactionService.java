package com.sudh.accord.service;

import com.sudh.accord.entity.Transaction;
import com.sudh.accord.entity.User;
import com.sudh.accord.enums.TransactionType;
import com.sudh.accord.repository.TransactionRepository;
import com.sudh.accord.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // "Wallet low" is defined relative to the user's own monthlyBudget rather
    // than an absolute rupee amount, since that's the only per-user scale we
    // have. Users without a configured (positive) budget are excluded rather
    // than guessed at.
    private static final BigDecimal WALLET_LOW_THRESHOLD_RATIO = new BigDecimal("0.20");
    private static final long NO_RECENT_TASK_DAYS = 3;

    public boolean isWalletUnderPressure(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();

        BigDecimal monthlyBudget = user.getMonthlyBudget();
        if (monthlyBudget == null || monthlyBudget.signum() <= 0) {
            return false;
        }

        BigDecimal balance = getBudget(userId);
        BigDecimal lowThreshold = monthlyBudget.multiply(WALLET_LOW_THRESHOLD_RATIO);
        boolean walletLow = balance.compareTo(lowThreshold) < 0;
        if (!walletLow) {
            return false;
        }

        LocalDateTime lastTaskCompletedAt =
                transactionRepository.findLatestTransactionDate(userId, TransactionType.TASK_COMPLETED);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(NO_RECENT_TASK_DAYS);

        // Never having completed a task also counts as "no task logged" —
        // this isn't just for users already in a pressure spiral.
        return lastTaskCompletedAt == null || lastTaskCompletedAt.isBefore(cutoff);
    }

    // wallet_low_days input for the Sunday narrative: how many of the 7 days
    // in [startInclusive, endExclusive) ended with balance below the same
    // 20%-of-monthlyBudget threshold isWalletUnderPressure uses. Users with
    // no configured (positive) budget can't have a threshold, so they score 0
    // rather than being guessed at — same convention as isWalletUnderPressure.
    //
    // There's no daily balance snapshot table, so this reconstructs each
    // day's end-of-day balance by seeding a running total from everything
    // before the range (getSumByTypeBefore) and then walking forward one day
    // at a time, applying that day's net transactions in order.
    public int countWalletLowDaysInRange(UUID userId, LocalDate startInclusive, LocalDate endExclusive) {
        User user = userRepository.findById(userId).orElseThrow();

        BigDecimal monthlyBudget = user.getMonthlyBudget();
        if (monthlyBudget == null || monthlyBudget.signum() <= 0) {
            return 0;
        }
        BigDecimal lowThreshold = monthlyBudget.multiply(WALLET_LOW_THRESHOLD_RATIO);

        LocalDateTime rangeStart = startInclusive.atStartOfDay();
        LocalDateTime rangeEnd = endExclusive.atStartOfDay();

        BigDecimal earnedBefore = transactionRepository.getSumByTypeBefore(userId, TransactionType.TASK_COMPLETED, rangeStart);
        BigDecimal spentBefore = transactionRepository.getSumByTypeBefore(userId, TransactionType.PAYMENT_MADE, rangeStart);
        BigDecimal runningBalance = nz(earnedBefore).subtract(nz(spentBefore));

        List<Transaction> rangeTransactions =
                transactionRepository.findAllByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, rangeStart, rangeEnd);

        Map<LocalDate, BigDecimal> netByDay = new LinkedHashMap<>();
        for (Transaction t : rangeTransactions) {
            BigDecimal signedAmount = t.getType() == TransactionType.TASK_COMPLETED
                    ? t.getAmount()
                    : t.getAmount().negate();
            netByDay.merge(t.getCreatedAt().toLocalDate(), signedAmount, BigDecimal::add);
        }

        int lowDays = 0;
        for (LocalDate day = startInclusive; day.isBefore(endExclusive); day = day.plusDays(1)) {
            runningBalance = runningBalance.add(netByDay.getOrDefault(day, BigDecimal.ZERO));
            if (runningBalance.compareTo(lowThreshold) < 0) {
                lowDays++;
            }
        }
        return lowDays;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}