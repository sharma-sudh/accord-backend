package com.sudh.accord.controller;

import com.sudh.accord.dto.NarrativeResponse;
import com.sudh.accord.repository.WeeklyNarrativeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/narrative")
public class NarrativeController {

    private final WeeklyNarrativeRepository weeklyNarrativeRepository;

    public NarrativeController(WeeklyNarrativeRepository weeklyNarrativeRepository) {
        this.weeklyNarrativeRepository = weeklyNarrativeRepository;
    }

    // Delivery fallback: no FCM infra exists yet, so instead of the server
    // pushing the Sunday narrative, the Android client polls this once on
    // app open (mirrors StreakController#checkWalletPressure's polling
    // pattern) and shows a local notification the first time it sees a
    // narrative it hasn't shown before. 204 covers both "new user" and
    // "before this week's Sunday run has happened yet".
    @GetMapping("/latest")
    public ResponseEntity<NarrativeResponse> getLatest(@AuthenticationPrincipal String userId) {
        return weeklyNarrativeRepository.findTopByUserIdOrderByWeekStartDateDesc(UUID.fromString(userId))
                .map(narrative -> ResponseEntity.ok(new NarrativeResponse(
                        narrative.getNarrativeText(),
                        narrative.getWeekStartDate().toString(),
                        narrative.getGeneratedAt() != null ? narrative.getGeneratedAt().toString() : null
                )))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}