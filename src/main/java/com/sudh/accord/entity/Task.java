package com.sudh.accord.entity;

import com.sudh.accord.enums.TaskType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    private TaskType type;

    @Column(columnDefinition = "boolean default false")
    private boolean isCompleted;

    private LocalDate dueDate;

    private LocalDateTime lastCompletedAt;

    // Version vector for offline sync (0.3.0): bumped on every server-side
    // mutation (via /complete or /sync), compared against the client's
    // baseVersion to detect concurrent edits.
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int version = 0;

    // Comma-separated field names touched by whichever mutation last bumped
    // `version`. Lets a later /sync call tell whether an intervening change
    // overlaps with the incoming one, without needing a full change-log table.
    // Null/blank means the task has never been mutated since creation.
    private String lastChangedFields;

    // Added for analytics (0.2.0): completionRate needs "tasks created in range".
    // Heads up: ddl-auto=update just ALTERs the table to add a nullable column —
    // it does NOT backfill existing rows, so any task created before this change
    // will have createdAt = null. AnalyticsService treats null as "not in range"
    // for the denominator, which is correct behavior, just flagging it since it's
    // not a normal @CreationTimestamp guarantee for old data.
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")   // FK column in books table
    private User user;

    public Task() {
    }

    public Task(UUID id,
                String title,
                String description,
                BigDecimal value,
                TaskType type,
                boolean isCompleted,
                LocalDate dueDate,
                LocalDateTime lastCompletedAt,
                User user) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.value = value;
        this.type = type;
        this.isCompleted = isCompleted;
        this.dueDate = dueDate;
        this.lastCompletedAt = lastCompletedAt;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public boolean getCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getLastCompletedAt() {
        return lastCompletedAt;
    }

    public void setLastCompletedAt(LocalDateTime lastCompletedAt) {
        this.lastCompletedAt = lastCompletedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getLastChangedFields() {
        return lastChangedFields;
    }

    public void setLastChangedFields(String lastChangedFields) {
        this.lastChangedFields = lastChangedFields;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
        Task task = (Task) o;
        return Objects.equals(id, task.id) && Objects.equals(title, task.title) && Objects.equals(description, task.description) && Objects.equals(value, task.value) && type == task.type && Objects.equals(isCompleted, task.isCompleted) && Objects.equals(dueDate, task.dueDate) && Objects.equals(lastCompletedAt, task.lastCompletedAt) && Objects.equals(createdAt, task.createdAt) && Objects.equals(user, task.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, value, type, isCompleted, dueDate, lastCompletedAt, createdAt, user);
    }
}