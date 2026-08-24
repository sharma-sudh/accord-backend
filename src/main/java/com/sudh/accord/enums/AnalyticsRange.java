package com.sudh.accord.enums;

public enum AnalyticsRange {
    WEEK(7), MONTH(30);

    private final int days;

    AnalyticsRange(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    // Accepts case-insensitive query params ("week", "WEEK", "Week") rather than
    // relying on Spring's default enum binding, which is case-sensitive and would
    // otherwise make ?range=week 400 while ?range=WEEK works — surprising for API
    // consumers.
    public static AnalyticsRange from(String value) {
        if (value == null || value.isBlank()) {
            return WEEK;
        }
        try {
            return AnalyticsRange.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("range must be 'week' or 'month', got: " + value);
        }
    }
}
