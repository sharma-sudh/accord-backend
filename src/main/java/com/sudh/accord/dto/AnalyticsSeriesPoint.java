package com.sudh.accord.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnalyticsSeriesPoint(
        LocalDate date,
        BigDecimal earned,
        BigDecimal spent,
        long completedCount
) {}
