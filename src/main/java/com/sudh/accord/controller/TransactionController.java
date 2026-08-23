package com.sudh.accord.controller;

import com.sudh.accord.service.TransactionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // No public POST here — Transaction creation is server-authoritative only.
    // It happens internally from TaskService.completeTask, and later from the
    // payment-deduction flow (0.3.0) and any admin/scheduled job. Do not add a
    // client-facing endpoint that accepts a raw Transaction (type/user must
    // never be client-settable). If a "log a payment" client action is needed,
    // add a narrow POST /payment endpoint that takes only { amount, merchantName },
    // and builds the Transaction server-side with type = PAYMENT_MADE and user
    // from the JWT.

    @GetMapping("/balance")
    public BigDecimal getBudget(@AuthenticationPrincipal String userId){
        return transactionService.getBudget(UUID.fromString(userId));
    }
}