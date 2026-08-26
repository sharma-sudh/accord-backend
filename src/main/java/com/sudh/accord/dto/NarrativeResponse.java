package com.sudh.accord.dto;

public record NarrativeResponse(
        String narrative,
        String weekStartDate,
        String generatedAt
) {
}