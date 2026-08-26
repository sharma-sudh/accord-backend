package com.sudh.accord.repository;

import com.sudh.accord.entity.WeeklyNarrative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyNarrativeRepository extends JpaRepository<WeeklyNarrative, UUID> {

    // Backs GET /api/v1/narrative/latest — the Android "fetch on next app
    // open" fallback for users without FCM delivery.
    Optional<WeeklyNarrative> findTopByUserIdOrderByWeekStartDateDesc(UUID userId);

    // Idempotency guard: if the Sunday cron is ever re-triggered (manual
    // re-run, retry after a partial-batch failure) for a week already
    // generated, skip re-calling Gemini for that user rather than double
    // billing/writing.
    boolean existsByUserIdAndWeekStartDate(UUID userId, LocalDate weekStartDate);
}