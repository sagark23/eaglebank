package com.eaglebank.service;

import com.eaglebank.domain.Address;
import com.eaglebank.domain.BankAccount;
import com.eaglebank.domain.Transaction;
import com.eaglebank.domain.User;
import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.response.ListTransactionsResponse;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.exception.ForbiddenException;
import com.eaglebank.exception.InsufficientFundsException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.repository.BankAccountRepository;
import com.eaglebank.repository.TransactionRepository;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldCreateDepositSuccessfully() {
        // Given
        String accountNumber = "01234567";
        String userId = "usr-abc123";
        BigDecimal depositAmount = new BigDecimal("100.00");
        BigDecimal initialBalance = new BigDecimal("50.00");

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(depositAmount)
                .currency("GBP")
                .type("deposit")
                .reference("Salary payment")
                .build();

        User user = createUser(userId);
        BankAccount account = createAccount(accountNumber, user, initialBalance);

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(idGenerator.generateTransactionId()).thenReturn("tan-xyz789");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            transaction.setCreatedAt(LocalDateTime.now());
            return transaction;
        });

        // When
        TransactionResponse response = transactionService.createTransaction(accountNumber, request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("tan-xyz789");
        assertThat(response.amount()).isEqualByComparingTo(depositAmount);
        assertThat(response.currency()).isEqualTo("GBP");
        assertThat(response.type()).isEqualTo("deposit");
        assertThat(response.reference()).isEqualTo("Salary payment");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.createdTimestamp()).isNotNull();

        // Verify balance was updated
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(idGenerator).generateTransactionId();
        verify(transactionRepository).save(any(Transaction.class));
        verify(bankAccountRepository).save(account);
    }

    @Test
    void shouldCreateWithdrawalSuccessfully() {
        // Given
        String accountNumber = "01234567";
        String userId = "usr-abc123";
        BigDecimal withdrawalAmount = new BigDecimal("30.00");
        BigDecimal initialBalance = new BigDecimal("100.00");

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(withdrawalAmount)
                .currency("GBP")
                .type("withdrawal")
                .reference("ATM withdrawal")
                .build();

        User user = createUser(userId);
        BankAccount account = createAccount(accountNumber, user, initialBalance);

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(idGenerator.generateTransactionId()).thenReturn("tan-xyz790");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(2L);
            transaction.setCreatedAt(LocalDateTime.now());
            return transaction;
        });

        // When
        TransactionResponse response = transactionService.createTransaction(accountNumber, request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("tan-xyz790");
        assertThat(response.amount()).isEqualByComparingTo(withdrawalAmount);
        assertThat(response.type()).isEqualTo("withdrawal");

        // Verify balance was updated
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));

        verify(transactionRepository).save(any(Transaction.class));
        verify(bankAccountRepository).save(account);
    }

    @Test
    void shouldThrowUnprocessableEntityExceptionWhenInsufficientFunds() {
        // Given
        String accountNumber = "01234567";
        String userId = "usr-abc123";
        BigDecimal withdrawalAmount = new BigDecimal("150.00");
        BigDecimal initialBalance = new BigDecimal("100.00");

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(withdrawalAmount)
                .currency("GBP")
                .type("withdrawal")
                .build();

        User user = createUser(userId);
        BankAccount account = createAccount(accountNumber, user, initialBalance);

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(accountNumber, request, userId))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(bankAccountRepository, never()).save(account);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenAccountNotFound() {
        // Given
        String accountNumber = "01999999";
        String userId = "usr-abc123";

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("deposit")
                .build();

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.createTransaction(accountNumber, request, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldListTransactionsSuccessfully() {
        // Given
        String accountNumber = "01234567";
        String userId = "usr-abc123";

        User user = createUser(userId);
        BankAccount account = createAccount(accountNumber, user, new BigDecimal("100.00"));

        Transaction transaction1 = createTransaction("tan-abc1", new BigDecimal("50.00"),
                Transaction.TransactionType.DEPOSIT, account, user);
        Transaction transaction2 = createTransaction("tan-abc2", new BigDecimal("25.00"),
                Transaction.TransactionType.WITHDRAWAL, account, user);

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccount_AccountNumberOrderByCreatedAtDesc(accountNumber))
                .thenReturn(Arrays.asList(transaction2, transaction1));

        // When
        ListTransactionsResponse response = transactionService.listTransactions(accountNumber, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.transactions()).hasSize(2);
        assertThat(response.transactions().get(0).id()).isEqualTo("tan-abc2");
        assertThat(response.transactions().get(1).id()).isEqualTo("tan-abc1");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(transactionRepository).findByAccount_AccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    @Test
    void shouldGetTransactionSuccessfully() {
        // Given
        String accountNumber = "01234567";
        String transactionId = "tan-abc123";
        String userId = "usr-abc123";

        User user = createUser(userId);
        BankAccount account = createAccount(accountNumber, user, new BigDecimal("100.00"));
        Transaction transaction = createTransaction(transactionId, new BigDecimal("50.00"),
                Transaction.TransactionType.DEPOSIT, account, user);

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByTransactionIdAndAccount_AccountNumber(transactionId, accountNumber))
                .thenReturn(Optional.of(transaction));

        // When
        TransactionResponse response = transactionService.getTransaction(accountNumber, transactionId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(transactionId);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50.00"));

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(transactionRepository).findByTransactionIdAndAccount_AccountNumber(transactionId, accountNumber);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenTransactionNotFound() {
        // Given
        String accountNumber = "01234567";
        String transactionId = "tan-notfound";
        String userId = "usr-abc123";

        User user = createUser(userId);
        BankAccount account = createAccount(accountNumber, user, new BigDecimal("100.00"));

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(transactionRepository.findByTransactionIdAndAccount_AccountNumber(transactionId, accountNumber))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getTransaction(accountNumber, transactionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(transactionRepository).findByTransactionIdAndAccount_AccountNumber(transactionId, accountNumber);
    }

    @Test
    void shouldCreateTransactionWithoutReference() {
        // Given
        String accountNumber = "01234567";
        String userId = "usr-abc123";

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("75.00"))
                .currency("GBP")
                .type("deposit")
                .build();

        User user = createUser(userId);
        BankAccount account = createAccount(accountNumber, user, new BigDecimal("25.00"));

        when(bankAccountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(idGenerator.generateTransactionId()).thenReturn("tan-xyz791");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(3L);
            transaction.setCreatedAt(LocalDateTime.now());
            return transaction;
        });

        // When
        TransactionResponse response = transactionService.createTransaction(accountNumber, request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.reference()).isNull();

        verify(transactionRepository).save(any(Transaction.class));
    }

    // Helper methods
    private User createUser(String userId) {
        return User.builder()
                .id(1L)
                .userId(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .phoneNumber("+447123456789")
                .address(Address.builder()
                        .line1("123 Main St")
                        .town("London")
                        .county("Greater London")
                        .postcode("SW1A 1AA")
                        .build())
                .build();
    }

    private BankAccount createAccount(String accountNumber, User user, BigDecimal balance) {
        return BankAccount.builder()
                .id(1L)
                .accountNumber(accountNumber)
                .sortCode("10-10-10")
                .name("Test Account")
                .accountType(BankAccount.AccountType.PERSONAL)
                .balance(balance)
                .currency("GBP")
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Transaction createTransaction(String transactionId, BigDecimal amount,
                                         Transaction.TransactionType type, BankAccount account, User user) {
        return Transaction.builder()
                .id(1L)
                .transactionId(transactionId)
                .amount(amount)
                .currency("GBP")
                .type(type)
                .reference("Test transaction")
                .account(account)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

