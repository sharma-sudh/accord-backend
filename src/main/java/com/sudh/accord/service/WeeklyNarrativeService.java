package com.sudh.accord.service;

import com.sudh.accord.dto.NarrativeInput;
import com.sudh.accord.entity.User;
import com.sudh.accord.entity.WeeklyNarrative;
import com.sudh.accord.enums.TransactionType;
import com.sudh.accord.repository.TransactionRepository;
import com.sudh.accord.repository.UserRepository;
import com.sudh.accord.repository.WeeklyNarrativeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class WeeklyNarrativeService {

    // Matches StreakService / application.properties' hibernate.jdbc.time_zone
    // convention: "week" boundaries are computed in the users' own timezone,
    // not the server's default.
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final WeeklyNarrativeRepository weeklyNarrativeRepository;
    private final TransactionService transactionService;
    private final GeminiNarrativeClient geminiNarrativeClient;

    public WeeklyNarrativeService(UserRepository userRepository,
                                  TransactionRepository transactionRepository,
                                  WeeklyNarrativeRepository weeklyNarrativeRepository,
                                  TransactionService transactionService,
                                  GeminiNarrativeClient geminiNarrativeClient) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.weeklyNarrativeRepository = weeklyNarrativeRepository;
        this.transactionService = transactionService;
        this.geminiNarrativeClient = geminiNarrativeClient;
    }

    /**
     * Builds the week's {earned, spent, tasks_completed, wallet_low_days,
     * streak_days} input, calls Gemini, and persists the result. Returns null
     * (a no-op, not an error) if this user already has a narrative for
     * weekStart — see WeeklyNarrativeRepository's idempotency note.
     * <p>
     * Deliberately not @Transactional-wrapping the Gemini HTTP call itself:
     * that's a slow external network call, and holding a DB transaction open
     * across it for every user in the batch would exhaust the connection
     * pool. The read queries below and the final save are each their own
     * short-lived unit of work.
     */
    public WeeklyNarrative generateForUser(UUID userId, LocalDate weekStartInclusive, LocalDate weekEndExclusive) {
        if (weeklyNarrativeRepository.existsByUserIdAndWeekStartDate(userId, weekStartInclusive)) {
            return null;
        }

        NarrativeInput input = buildInput(userId, weekStartInclusive, weekEndExclusive);
        String narrativeText = geminiNarrativeClient.generateNarrative(input);

        return saveNarrative(userId, weekStartInclusive, narrativeText);
    }

    @Transactional(readOnly = true)
    NarrativeInput buildInput(UUID userId, LocalDate weekStartInclusive, LocalDate weekEndExclusive) {
        User user = userRepository.findById(userId).orElseThrow();

        LocalDateTime start = weekStartInclusive.atStartOfDay();
        LocalDateTime end = weekEndExclusive.atStartOfDay();

        BigDecimal earned = nz(transactionRepository.getSumByTypeBetween(userId, TransactionType.TASK_COMPLETED, start, end));
        BigDecimal spent = nz(transactionRepository.getSumByTypeBetween(userId, TransactionType.PAYMENT_MADE, start, end));
        long tasksCompleted = transactionRepository.countByUserIdAndTypeAndCreatedAtBetween(
                userId, TransactionType.TASK_COMPLETED, start, end);
        int walletLowDays = transactionService.countWalletLowDaysInRange(userId, weekStartInclusive, weekEndExclusive);
        int streakDays = user.getCurrentStreak();

        return new NarrativeInput(earned, spent, tasksCompleted, walletLowDays, streakDays);
    }

    @Transactional
    WeeklyNarrative saveNarrative(UUID userId, LocalDate weekStartInclusive, String narrativeText) {
        User user = userRepository.findById(userId).orElseThrow();
        WeeklyNarrative narrative = new WeeklyNarrative(null, user, weekStartInclusive, narrativeText);
        return weeklyNarrativeRepository.save(narrative);
    }

    /** The past 7 completed days, e.g. run on Sunday morning this covers last Sunday through Saturday. */
    public static LocalDate currentWeekStart() {
        return LocalDate.now(ZONE).minusDays(7);
    }

    public static LocalDate currentWeekEndExclusive() {
        return LocalDate.now(ZONE);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}