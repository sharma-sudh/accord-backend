package com.sudh.accord.entity;

import com.sudh.accord.enums.TaskType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                User user) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.value = value;
        this.type = type;
        this.isCompleted = isCompleted;
        this.dueDate = dueDate;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id) && Objects.equals(title, task.title) && Objects.equals(description, task.description) && Objects.equals(value, task.value) && type == task.type && Objects.equals(isCompleted, task.isCompleted) && Objects.equals(dueDate, task.dueDate) && Objects.equals(user, task.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, value, type, isCompleted, dueDate, user);
    }
}
