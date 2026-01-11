package com.eaglebank.service;

import com.eaglebank.domain.BankAccount;
import com.eaglebank.domain.User;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.UpdateBankAccountRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.ListBankAccountsResponse;
import com.eaglebank.exception.ConflictException;
import com.eaglebank.exception.ForbiddenException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountService {

    private static final String EAGLE_BANK_SORT_CODE = "10-10-10";
    private static final String DEFAULT_CURRENCY = "GBP";

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final IdGenerator idGenerator;

    public BankAccountResponse createAccount(String userId, CreateBankAccountRequest request) {
        log.debug("Creating bank account for user: {}", userId);

        // Verify user exists
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Generate unique account number
        String accountNumber = generateUniqueAccountNumber();

        BankAccount account = BankAccount.builder()
                .accountNumber(accountNumber)
                .sortCode(EAGLE_BANK_SORT_CODE)
                .name(request.name())
                .accountType(BankAccount.AccountType.valueOf(request.accountType().toUpperCase()))
                .balance(BigDecimal.ZERO)
                .currency(DEFAULT_CURRENCY)
                .user(user)
                .build();

        BankAccount saved = bankAccountRepository.save(account);
        log.info("Bank account created successfully: {} for user: {}", saved.getAccountNumber(), userId);

        return BankAccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ListBankAccountsResponse listAccounts(String userId) {
        log.debug("Listing accounts for user: {}", userId);

        List<BankAccount> accounts = bankAccountRepository.findByUserUserId(userId);
        List<BankAccountResponse> accountResponses = accounts.stream()
                .map(BankAccountResponse::from)
                .toList();

        return new ListBankAccountsResponse(accountResponses);
    }

    @Transactional(readOnly = true)
    public boolean hasAccounts(String userId) {
        log.debug("Checking if user has accounts: {}", userId);
        return bankAccountRepository.countByUserUserId(userId) > 0;
    }

    @Transactional(readOnly = true)
    public BankAccountResponse getAccountByAccountNumber(String userId, String accountNumber) {
        log.debug("Fetching account: {} for user: {}", accountNumber, userId);

        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found with account number: " + accountNumber));

        // Authorization check
        if (!account.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to access this bank account");
        }

        return BankAccountResponse.from(account);
    }

    public BankAccountResponse updateAccount(String userId, String accountNumber, UpdateBankAccountRequest request) {
        log.debug("Updating account: {} for user: {}", accountNumber, userId);

        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found with account number: " + accountNumber));

        // Authorization check
        if (!account.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to update this bank account");
        }

        // Update fields if provided
        if (request.name() != null && !request.name().isBlank()) {
            account.updateName(request.name());
        }

        if (request.accountType() != null && !request.accountType().isBlank()) {
            account.updateAccountType(BankAccount.AccountType.valueOf(request.accountType().toUpperCase()));
        }

        BankAccount updated = bankAccountRepository.save(account);
        log.info("Bank account updated successfully: {}", accountNumber);

        return BankAccountResponse.from(updated);
    }

    public void deleteAccount(String userId, String accountNumber) {
        log.debug("Deleting account: {} for user: {}", accountNumber, userId);

        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found with account number: " + accountNumber));

        // Authorization check
        if (!account.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to delete this bank account");
        }

        bankAccountRepository.delete(account);
        log.info("Bank account deleted successfully: {}", accountNumber);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        do {
            accountNumber = idGenerator.generateAccountNumber();
            attempts++;
            if (attempts > 100) {
                throw new ConflictException("Unable to generate unique account number");
            }
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}

