package com.sudh.accord.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private BigDecimal monthlyBudget;

    private String passwordHash;  // null for Google users

    private String googleId;      // null for email users

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Streak tracking (V1): reuses User instead of a separate DailyCheckIn entity.
    // lastCheckInDate is the calendar date (Asia/Kolkata, per hibernate.jdbc.time_zone)
    // of the most recent successful /streak/checkin call; null until the user's first check-in.
    private LocalDate lastCheckInDate;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int currentStreak = 0;

    public User() {
    }

    public User(UUID id,
                String name,
                String email,
                BigDecimal monthlyBudget,
                String passwordHash,
                String googleId,
                LocalDateTime createdAt,
                LocalDate lastCheckInDate,
                int currentStreak) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.monthlyBudget = monthlyBudget;
        this.passwordHash = passwordHash;
        this.googleId = googleId;
        this.createdAt = createdAt;
        this.lastCheckInDate = lastCheckInDate;
        this.currentStreak = currentStreak;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getLastCheckInDate() {
        return lastCheckInDate;
    }

    public void setLastCheckInDate(LocalDate lastCheckInDate) {
        this.lastCheckInDate = lastCheckInDate;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return currentStreak == user.currentStreak && Objects.equals(id, user.id) && Objects.equals(name, user.name) && Objects.equals(email, user.email) && Objects.equals(monthlyBudget, user.monthlyBudget) && Objects.equals(passwordHash, user.passwordHash) && Objects.equals(googleId, user.googleId) && Objects.equals(createdAt, user.createdAt) && Objects.equals(lastCheckInDate, user.lastCheckInDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, monthlyBudget, passwordHash, googleId, createdAt, lastCheckInDate, currentStreak);
    }
}