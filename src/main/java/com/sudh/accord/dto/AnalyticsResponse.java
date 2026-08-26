package com.sudh.accord.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        BigDecimal totalEarned,
        BigDecimal totalSpent,
        // Ratio 0.0–1.0, not a percentage. 0.0 when no tasks were created in
        // range (avoids a div-by-zero rather than signaling "0% completion").
        double completionRate,
        // The user's current daily check-in streak (User.currentStreak), as
        // of when this response was built — not scoped to `range` the way
        // the other fields are. Integer (not int) kept for API stability;
        // in practice always populated now that 0.4.0's streak logic exists.
        Integer streakDays,
        // One entry per day in the range, oldest first, zero-filled for days
        // with no activity — safe to feed straight into a chart.
        List<AnalyticsSeriesPoint> series,
        // Task title -> number of completions in range, for the most/least
        // completed breakdown. Only reflects tasks completed at least once;
        // never-completed tasks simply won't appear as a key.
        Map<String, Long> taskBreakdown,
        // True only when the user has zero transactions ever (not just zero in
        // the selected range) — drives the blurred/greyed empty-state chart.
        boolean isEmpty
) {}