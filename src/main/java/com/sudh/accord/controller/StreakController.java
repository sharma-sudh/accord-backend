package com.sudh.accord.controller;

import com.sudh.accord.dto.StreakResponse;
import com.sudh.accord.entity.User;
import com.sudh.accord.service.StreakService;
import com.sudh.accord.service.TransactionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/streak")
public class StreakController {

    private final StreakService streakService;
    private final TransactionService transactionService;

    public StreakController(StreakService streakService, TransactionService transactionService) {
        this.streakService = streakService;
        this.transactionService = transactionService;
    }

    @PostMapping("/checkin")
    public StreakResponse checkIn(@AuthenticationPrincipal String userId) {
        return toResponse(streakService.checkIn(UUID.fromString(userId)));
    }

    @GetMapping
    public StreakResponse getStreak(@AuthenticationPrincipal String userId) {
        return toResponse(streakService.getStreak(UUID.fromString(userId)));
    }

    // Pragmatic V1: client polls this once daily via WorkManager rather than
    // the server pushing via FCM. True means "wallet is low relative to the
    // user's monthlyBudget AND no task has been completed in 3+ days" — see
    // TransactionService.isWalletUnderPressure for the exact thresholds.
    @GetMapping("/wallet-pressure-check")
    public Boolean checkWalletPressure(@AuthenticationPrincipal String userId) {
        return transactionService.isWalletUnderPressure(UUID.fromString(userId));
    }

    private StreakResponse toResponse(User user) {
        return new StreakResponse(
                user.getCurrentStreak(),
                user.getLastCheckInDate() != null ? user.getLastCheckInDate().toString() : null
        );
    }
}