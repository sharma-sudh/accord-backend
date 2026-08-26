package com.sudh.accord.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

// Exactly the five fields the design doc specifies as Gemini's structured
// input for the Sunday narrative. @JsonProperty pins the wire format to
// snake_case regardless of Java field naming conventions, since this is
// serialized as-is into the prompt content sent to Gemini.
public record NarrativeInput(
        @JsonProperty("earned") BigDecimal earned,
        @JsonProperty("spent") BigDecimal spent,
        @JsonProperty("tasks_completed") long tasksCompleted,
        @JsonProperty("wallet_low_days") int walletLowDays,
        @JsonProperty("streak_days") int streakDays
) {
}