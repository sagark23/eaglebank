package com.eaglebank.controller;

import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.ListTransactionsResponse;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.security.CustomUserDetails;
import com.eaglebank.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts/{accountNumber}/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable String accountNumber,
            @Valid @RequestBody CreateTransactionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("POST /v1/accounts/{}/transactions - Creating transaction", accountNumber);
        String userId = currentUser.getUserId();
        TransactionResponse response = transactionService.createTransaction(accountNumber, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ListTransactionsResponse> listTransactions(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("GET /v1/accounts/{}/transactions - Listing transactions", accountNumber);
        String userId = currentUser.getUserId();
        ListTransactionsResponse response = transactionService.listTransactions(accountNumber, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String accountNumber,
            @PathVariable String transactionId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("GET /v1/accounts/{}/transactions/{} - Getting transaction", accountNumber, transactionId);
        String userId = currentUser.getUserId();
        TransactionResponse response = transactionService.getTransaction(accountNumber, transactionId, userId);
        return ResponseEntity.ok(response);
    }
}

