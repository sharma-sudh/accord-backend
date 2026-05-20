package com.sudh.accord.entity;

import com.sudh.accord.enums.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_task")
    private Task task;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private LocalDateTime clientTimestamp;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(UUID id,
                       BigDecimal amount,
                       User user,
                       Task task,
                       TransactionType type,
                       LocalDateTime clientTimestamp,
                       LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.user = user;
        this.task = task;
        this.type = type;
        this.clientTimestamp = clientTimestamp;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public LocalDateTime getClientTimestamp() {
        return clientTimestamp;
    }

    public void setClientTimestamp(LocalDateTime clientTimestamp) {
        this.clientTimestamp = clientTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id) && Objects.equals(amount, that.amount) && Objects.equals(user, that.user) && Objects.equals(task, that.task) && type == that.type && Objects.equals(clientTimestamp, that.clientTimestamp) && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, amount, user, task, type, clientTimestamp, createdAt);
    }
}
