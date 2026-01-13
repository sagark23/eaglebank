package com.eaglebank.controller;

import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.UpdateBankAccountRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.ListBankAccountsResponse;
import com.eaglebank.security.CustomUserDetails;
import com.eaglebank.service.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponse createAccount(
            @Valid @RequestBody CreateBankAccountRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Create account request for user: {}", currentUser.getUserId());
        return accountService.createAccount(currentUser.getUserId(), request);
    }

    @GetMapping
    public ListBankAccountsResponse listAccounts(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("List accounts request for user: {}", currentUser.getUserId());
        return accountService.listAccounts(currentUser.getUserId());
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("@securityService.ownsAccount(#accountNumber)")
    public BankAccountResponse getAccountByAccountNumber(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Get account request for accountNumber: {} by user: {}", accountNumber, currentUser.getUserId());
        return accountService.getAccountByAccountNumber(currentUser.getUserId(), accountNumber);
    }

    @PatchMapping("/{accountNumber}")
    @PreAuthorize("@securityService.ownsAccount(#accountNumber)")
    public BankAccountResponse updateAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateBankAccountRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Update account request for accountNumber: {} by user: {}", accountNumber, currentUser.getUserId());
        return accountService.updateAccount(currentUser.getUserId(), accountNumber, request);
    }

    @DeleteMapping("/{accountNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.ownsAccount(#accountNumber)")
    public void deleteAccount(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Delete account request for accountNumber: {} by user: {}", accountNumber, currentUser.getUserId());
        accountService.deleteAccount(currentUser.getUserId(), accountNumber);
    }
}

