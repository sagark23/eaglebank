package com.eaglebank.service;

import com.eaglebank.domain.BankAccount;
import com.eaglebank.domain.Transaction;
import com.eaglebank.domain.User;
import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.ListTransactionsResponse;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.exception.ForbiddenException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.exception.UnprocessableEntityException;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final IdGenerator idGenerator;

    public TransactionResponse createTransaction(String accountNumber,
                                                  CreateTransactionRequest request,
                                                  String userId) {
        log.debug("Creating transaction for account: {}, user: {}", accountNumber, userId);

        // Find and validate account ownership
        BankAccount account = findAndValidateAccountOwnership(accountNumber, userId);

        // Validate transaction based on type
        Transaction.TransactionType type = parseTransactionType(request.type());

        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionId(idGenerator.generateTransactionId())
                .amount(request.amount())
                .currency(request.currency())
                .type(type)
                .reference(request.reference())
                .account(account)
                .user(account.getUser())
                .build();

        updateAccountBalance(account, type, request.amount());

        // Save transaction and account
        Transaction savedTransaction = transactionRepository.save(transaction);
        bankAccountRepository.save(account);

        log.info("Transaction created: {} for account: {}", savedTransaction.getTransactionId(), accountNumber);
        return TransactionResponse.from(savedTransaction);
    }

    @Transactional(readOnly = true)
    public ListTransactionsResponse listTransactions(String accountNumber, String userId) {
        log.debug("Listing transactions for account: {}, user: {}", accountNumber, userId);

        // Validate account ownership
        findAndValidateAccountOwnership(accountNumber, userId);

        List<Transaction> transactions = transactionRepository
                .findByAccount_AccountNumberOrderByCreatedAtDesc(accountNumber);

        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());

        return ListTransactionsResponse.builder()
                .transactions(transactionResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String accountNumber, String transactionId, String userId) {
        log.debug("Getting transaction: {} for account: {}, user: {}", transactionId, accountNumber, userId);

        // Validate account ownership
        findAndValidateAccountOwnership(accountNumber, userId);

        // Find transaction by transactionId and accountNumber
        Transaction transaction = transactionRepository
                .findByTransactionIdAndAccount_AccountNumber(transactionId, accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + transactionId + " for account: " + accountNumber));

        return TransactionResponse.from(transaction);
    }

    private BankAccount findAndValidateAccountOwnership(String accountNumber, String userId) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

        if (!account.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to access this account");
        }

        return account;
    }

    private Transaction.TransactionType parseTransactionType(String type) {
        try {
            return Transaction.TransactionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction type: " + type);
        }
    }


    private void updateAccountBalance(BankAccount account, Transaction.TransactionType type, BigDecimal amount) {
        if (type == Transaction.TransactionType.DEPOSIT) {
            account.deposit(amount);
            log.debug("Balance after deposit: {}", account.getBalance());
        } else if (type == Transaction.TransactionType.WITHDRAWAL) {
            account.withdraw(amount);
            log.debug("Balance after withdrawal: {}", account.getBalance());
        }
    }
}

