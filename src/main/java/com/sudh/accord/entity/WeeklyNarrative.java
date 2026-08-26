package com.sudh.accord.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// One row per (user, weekStartDate) — see the unique constraint below. Written
// once by WeeklyNarrativeScheduler on Sunday morning and never mutated after;
// read by NarrativeController for the Android "fetch on next app open"
// fallback path.
@Entity
@Table(
        name = "weekly_narratives",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start_date"})
)
public class WeeklyNarrative {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Monday of the ISO week this narrative summarizes (Asia/Kolkata), not the
    // Sunday it was generated on — makes "which week is this" unambiguous.
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(nullable = false, columnDefinition = "text")
    private String narrativeText;

    @CreationTimestamp
    private LocalDateTime generatedAt;

    public WeeklyNarrative() {
    }

    // generatedAt intentionally excluded — @CreationTimestamp manages it
    // purely via Hibernate on insert, same convention as Task.createdAt.
    public WeeklyNarrative(UUID id, User user, LocalDate weekStartDate, String narrativeText) {
        this.id = id;
        this.user = user;
        this.weekStartDate = weekStartDate;
        this.narrativeText = narrativeText;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public String getNarrativeText() {
        return narrativeText;
    }

    public void setNarrativeText(String narrativeText) {
        this.narrativeText = narrativeText;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        WeeklyNarrative that = (WeeklyNarrative) o;
        return Objects.equals(id, that.id) && Objects.equals(user, that.user) && Objects.equals(weekStartDate, that.weekStartDate) && Objects.equals(narrativeText, that.narrativeText) && Objects.equals(generatedAt, that.generatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, weekStartDate, narrativeText, generatedAt);
    }
}