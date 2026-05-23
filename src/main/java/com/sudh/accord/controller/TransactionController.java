package com.sudh.accord.controller;

import com.sudh.accord.entity.Transaction;
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

    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction){
        return transactionService.createTransaction(transaction);
    }

    @GetMapping("/balance")
    public BigDecimal getBudget(@AuthenticationPrincipal String userId){
        return transactionService.getBudget(UUID.fromString(userId));
    }
}
