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
}
