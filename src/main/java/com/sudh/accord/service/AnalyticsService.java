package com.sudh.accord.service;

import com.sudh.accord.dto.AnalyticsResponse;
import com.sudh.accord.dto.AnalyticsSeriesPoint;
import com.sudh.accord.entity.Transaction;
import com.sudh.accord.enums.AnalyticsRange;
import com.sudh.accord.enums.TransactionType;
import com.sudh.accord.repository.TaskRepository;
import com.sudh.accord.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final TaskRepository taskRepository;

    public AnalyticsService(TransactionRepository transactionRepository, TaskRepository taskRepository) {
        this.transactionRepository = transactionRepository;
        this.taskRepository = taskRepository;
    }

    // readOnly=true, and required here specifically: spring.jpa.open-in-view=false
    // means the Hibernate session closes as soon as the repository call returns,
    // so touching transaction.getTask().getTitle() below (a LAZY association)
    // outside a transaction would throw LazyInitializationException.
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(UUID userId, AnalyticsRange range) {
        boolean isEmpty = transactionRepository.countByUserId(userId) == 0;

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(range.getDays() - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Transaction> transactions =
                transactionRepository.findAllByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, start, end);

        // Zero-fill every day in the range up front so the client gets a
        // complete series regardless of activity, per the design doc's "don't
        // show a chart full of zeros [for new users], but do show a proper
        // zero-filled series for existing users with a quiet day/week".
        Map<LocalDate, BigDecimal> earnedByDay = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> spentByDay = new LinkedHashMap<>();
        Map<LocalDate, Long> completedByDay = new LinkedHashMap<>();
        for (int i = 0; i < range.getDays(); i++) {
            LocalDate day = startDate.plusDays(i);
            earnedByDay.put(day, BigDecimal.ZERO);
            spentByDay.put(day, BigDecimal.ZERO);
            completedByDay.put(day, 0L);
        }

        BigDecimal totalEarned = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        Map<String, Long> taskBreakdown = new LinkedHashMap<>();
        long tasksCompletedInRange = 0;

        for (Transaction transaction : transactions) {
            LocalDate day = transaction.getCreatedAt().toLocalDate();
            if (transaction.getType() == TransactionType.TASK_COMPLETED) {
                earnedByDay.merge(day, transaction.getAmount(), BigDecimal::add);
                completedByDay.merge(day, 1L, Long::sum);
                totalEarned = totalEarned.add(transaction.getAmount());
                tasksCompletedInRange++;
                if (transaction.getTask() != null) {
                    taskBreakdown.merge(transaction.getTask().getTitle(), 1L, Long::sum);
                }
            } else if (transaction.getType() == TransactionType.PAYMENT_MADE) {
                spentByDay.merge(day, transaction.getAmount(), BigDecimal::add);
                totalSpent = totalSpent.add(transaction.getAmount());
            }
        }

        List<AnalyticsSeriesPoint> series = earnedByDay.keySet().stream()
                .map(day -> new AnalyticsSeriesPoint(day, earnedByDay.get(day), spentByDay.get(day), completedByDay.get(day)))
                .toList();

        long tasksCreatedInRange = taskRepository.countByUserIdAndCreatedAtBetween(userId, start, end);
        // Note: a recurring task completed multiple times in range inflates the
        // numerator against a denominator that only counts NEW tasks created in
        // range — this metric is a rough "activity" signal, not a strict ratio.
        // That's inherent to the spec ("completed / created"), not a bug here;
        // clamped to [0,1] so a busy recurring task can't push the UI past 100%.
        double completionRate = tasksCreatedInRange == 0
                ? 0.0
                : Math.min(1.0, tasksCompletedInRange / (double) tasksCreatedInRange);

        return new AnalyticsResponse(
                totalEarned,
                totalSpent,
                completionRate,
                null, // streakDays — wire up once 0.4.0's streak logic exists
                series,
                taskBreakdown,
                isEmpty
        );
    }
}
