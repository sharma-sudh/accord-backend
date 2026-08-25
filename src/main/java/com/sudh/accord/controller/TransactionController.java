package com.sudh.accord.controller;

import com.sudh.accord.dto.PaymentRequest;
import com.sudh.accord.dto.TransactionResponse;
import com.sudh.accord.entity.Transaction;
import com.sudh.accord.service.TransactionService;
import org.springframework.http.HttpStatus;
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

    // Transaction creation is otherwise server-authoritative only (from
    // TaskService.completeTask, the payment-deduction flow, or an admin/
    // scheduled job). This is the one narrow, deliberate exception: it takes
    // only { amount, merchantName } and builds the Transaction server-side
    // with type = PAYMENT_MADE and user from the JWT — the client can never
    // set type or user directly.
    @PostMapping("/payment")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse logPayment(@RequestBody PaymentRequest request, @AuthenticationPrincipal String userId) {
        Transaction transaction = transactionService.createPayment(UUID.fromString(userId), request.amount(), request.merchantName());
        return toResponse(transaction);
    }

    @GetMapping("/balance")
    public BigDecimal getBudget(@AuthenticationPrincipal String userId){
        return transactionService.getBudget(UUID.fromString(userId));
    }

    // ── mapping ──────────────────────────────────────────────────────────────

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getMerchantName(),
                transaction.getCreatedAt() != null ? transaction.getCreatedAt().toString() : null
        );
    }
}