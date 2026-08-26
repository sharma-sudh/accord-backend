package com.sudh.accord.dto;

public record StreakResponse(
        int currentStreak,
        String lastCheckInDate
) {}