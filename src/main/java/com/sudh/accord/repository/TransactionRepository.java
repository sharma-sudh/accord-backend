package com.sudh.accord.repository;

import com.sudh.accord.entity.Transaction;
import com.sudh.accord.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionRepository  extends JpaRepository<Transaction, UUID> {
    @Query("select sum(e.amount) from Transaction e where e.type = :type and e.user.id = :userId")
    BigDecimal getSumByType(@Param("userId") UUID userId, @Param("type")TransactionType type);
}
