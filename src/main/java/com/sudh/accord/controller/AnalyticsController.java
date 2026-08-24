package com.sudh.accord.controller;

import com.sudh.accord.dto.AnalyticsResponse;
import com.sudh.accord.enums.AnalyticsRange;
import com.sudh.accord.service.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // ?range=week (default) or ?range=month. The response is fully aggregated —
    // the client just binds it to the chart/stat cards, no local summing or
    // bucketing. Deliberately no GET /transactions endpoint alongside this:
    // since analytics moved server-side, the client no longer needs raw
    // transaction data to compute anything itself.
    @GetMapping
    public AnalyticsResponse getAnalytics(
            @RequestParam(defaultValue = "week") String range,
            @AuthenticationPrincipal String userId) {
        return analyticsService.getAnalytics(UUID.fromString(userId), AnalyticsRange.from(range));
    }
}
