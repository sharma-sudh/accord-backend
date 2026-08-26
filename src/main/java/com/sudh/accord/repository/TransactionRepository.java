package com.sudh.accord.repository;

import com.sudh.accord.entity.Transaction;
import com.sudh.accord.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository  extends JpaRepository<Transaction, UUID> {
    @Query("select sum(e.amount) from Transaction e where e.type = :type and e.user.id = :userId")
    BigDecimal getSumByType(@Param("userId") UUID userId, @Param("type")TransactionType type);

    // Used for the isEmpty flag on /analytics — all-time count, not scoped to
    // any range, so a new user shows the empty state even if they picked a
    // range that happens to have no data for an existing user too.
    long countByUserId(UUID userId);

    // Backs the analytics series/breakdown. createdAt (server timestamp) is used
    // rather than clientTimestamp since the ledger is server-authoritative.
    List<Transaction> findAllByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID userId, LocalDateTime start, LocalDateTime end);

    // Backs the wallet-pressure check's "no task logged in 3+ days" leg. Null
    // means the user has never completed a task at all, which the service
    // layer treats as satisfying that condition too.
    @Query("select max(t.createdAt) from Transaction t where t.user.id = :userId and t.type = :type")
    LocalDateTime findLatestTransactionDate(@Param("userId") UUID userId, @Param("type") TransactionType type);

    // Starting balance for TransactionService.countWalletLowDaysInRange: sum
    // of a type strictly before a cutoff, so a running balance can be seeded
    // before walking day-by-day through the week itself.
    @Query("select sum(t.amount) from Transaction t where t.type = :type and t.user.id = :userId and t.createdAt < :before")
    BigDecimal getSumByTypeBefore(@Param("userId") UUID userId, @Param("type") TransactionType type, @Param("before") LocalDateTime before);

    // Weekly totals for the Sunday narrative's structured input (earned/spent).
    @Query("select sum(t.amount) from Transaction t where t.type = :type and t.user.id = :userId and t.createdAt >= :start and t.createdAt < :end")
    BigDecimal getSumByTypeBetween(@Param("userId") UUID userId, @Param("type") TransactionType type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // tasks_completed for the Sunday narrative's structured input.
    long countByUserIdAndTypeAndCreatedAtBetween(UUID userId, TransactionType type, LocalDateTime start, LocalDateTime end);
}