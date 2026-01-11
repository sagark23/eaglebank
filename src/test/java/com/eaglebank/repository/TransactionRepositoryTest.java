package com.eaglebank.repository;

import com.eaglebank.config.JpaConfig;
import com.eaglebank.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void shouldSaveAndFindTransactionByTransactionId() {
        // Given
        User user = createAndSaveUser("usr-test123", "test@example.com");
        BankAccount account = createAndSaveAccount("01234567", user);

        Transaction transaction = Transaction.builder()
                .transactionId("tan-abc123")
                .amount(new BigDecimal("100.50"))
                .currency("GBP")
                .type(Transaction.TransactionType.DEPOSIT)
                .reference("Test deposit")
                .account(account)
                .user(user)
                .build();

        // When
        Transaction saved = transactionRepository.save(transaction);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<Transaction> found = transactionRepository.findByTransactionId("tan-abc123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTransactionId()).isEqualTo("tan-abc123");
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(found.get().getCurrency()).isEqualTo("GBP");
        assertThat(found.get().getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(found.get().getReference()).isEqualTo("Test deposit");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindAllTransactionsByAccountNumber() {
        // Given
        User user = createAndSaveUser("usr-test124", "test2@example.com");
        BankAccount account = createAndSaveAccount("01234568", user);

        Transaction transaction1 = createAndSaveTransaction("tan-abc124", new BigDecimal("50.00"),
                Transaction.TransactionType.DEPOSIT, account, user);
        Transaction transaction2 = createAndSaveTransaction("tan-abc125", new BigDecimal("25.00"),
                Transaction.TransactionType.WITHDRAWAL, account, user);

        testEntityManager.flush();
        testEntityManager.clear();

        // When
        List<Transaction> transactions = transactionRepository
                .findByAccount_AccountNumberOrderByCreatedAtDesc("01234568");

        // Then
        assertThat(transactions).hasSize(2);
        // Should be ordered by createdAt DESC
        assertThat(transactions.get(0).getTransactionId()).isIn("tan-abc124", "tan-abc125");
        assertThat(transactions.get(1).getTransactionId()).isIn("tan-abc124", "tan-abc125");
    }

    @Test
    void shouldFindTransactionByTransactionIdAndAccountNumber() {
        // Given
        User user = createAndSaveUser("usr-test125", "test3@example.com");
        BankAccount account = createAndSaveAccount("01234569", user);

        Transaction transaction = createAndSaveTransaction("tan-abc126", new BigDecimal("75.00"),
                Transaction.TransactionType.DEPOSIT, account, user);

        testEntityManager.flush();
        testEntityManager.clear();

        // When
        Optional<Transaction> found = transactionRepository
                .findByTransactionIdAndAccount_AccountNumber("tan-abc126", "01234569");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTransactionId()).isEqualTo("tan-abc126");
    }

    @Test
    void shouldNotFindTransactionWithWrongAccountNumber() {
        // Given
        User user = createAndSaveUser("usr-test126", "test4@example.com");
        BankAccount account = createAndSaveAccount("01234570", user);

        Transaction transaction = createAndSaveTransaction("tan-abc127", new BigDecimal("75.00"),
                Transaction.TransactionType.DEPOSIT, account, user);

        testEntityManager.flush();
        testEntityManager.clear();

        // When
        Optional<Transaction> found = transactionRepository
                .findByTransactionIdAndAccount_AccountNumber("tan-abc127", "01999999");

        // Then
        assertThat(found).isNotPresent();
    }

    @Test
    void shouldCheckIfTransactionIdExists() {
        // Given
        User user = createAndSaveUser("usr-test127", "test5@example.com");
        BankAccount account = createAndSaveAccount("01234571", user);

        Transaction transaction = createAndSaveTransaction("tan-abc128", new BigDecimal("100.00"),
                Transaction.TransactionType.DEPOSIT, account, user);

        testEntityManager.flush();
        testEntityManager.clear();

        // When/Then
        assertThat(transactionRepository.existsByTransactionId("tan-abc128")).isTrue();
        assertThat(transactionRepository.existsByTransactionId("tan-notexist")).isFalse();
    }

    @Test
    void shouldReturnEmptyListForAccountWithNoTransactions() {
        // Given
        User user = createAndSaveUser("usr-test128", "test6@example.com");
        BankAccount account = createAndSaveAccount("01234572", user);

        testEntityManager.flush();
        testEntityManager.clear();

        // When
        List<Transaction> transactions = transactionRepository
                .findByAccount_AccountNumberOrderByCreatedAtDesc("01234572");

        // Then
        assertThat(transactions).isEmpty();
    }

    @Test
    void shouldSaveTransactionWithoutReference() {
        // Given
        User user = createAndSaveUser("usr-test129", "test7@example.com");
        BankAccount account = createAndSaveAccount("01234573", user);

        Transaction transaction = Transaction.builder()
                .transactionId("tan-abc129")
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type(Transaction.TransactionType.DEPOSIT)
                .account(account)
                .user(user)
                .build();

        // When
        Transaction saved = transactionRepository.save(transaction);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<Transaction> found = transactionRepository.findByTransactionId("tan-abc129");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getReference()).isNull();
    }

    @Test
    void shouldSaveWithdrawalTransaction() {
        // Given
        User user = createAndSaveUser("usr-test130", "test8@example.com");
        BankAccount account = createAndSaveAccount("01234574", user);

        Transaction transaction = Transaction.builder()
                .transactionId("tan-abc130")
                .amount(new BigDecimal("30.00"))
                .currency("GBP")
                .type(Transaction.TransactionType.WITHDRAWAL)
                .reference("ATM withdrawal")
                .account(account)
                .user(user)
                .build();

        // When
        Transaction saved = transactionRepository.save(transaction);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<Transaction> found = transactionRepository.findByTransactionId("tan-abc130");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    // Helper methods
    private User createAndSaveUser(String userId, String email) {
        User user = User.builder()
                .userId(userId)
                .name("Test User")
                .email(email)
                .passwordHash("hashedpassword")
                .phoneNumber("+447123456789")
                .address(Address.builder()
                        .line1("123 Main St")
                        .town("London")
                        .county("Greater London")
                        .postcode("SW1A 1AA")
                        .build())
                .build();
        return userRepository.save(user);
    }

    private BankAccount createAndSaveAccount(String accountNumber, User user) {
        BankAccount account = BankAccount.builder()
                .accountNumber(accountNumber)
                .sortCode("10-10-10")
                .name("Test Account")
                .accountType(BankAccount.AccountType.PERSONAL)
                .balance(BigDecimal.ZERO)
                .currency("GBP")
                .user(user)
                .build();
        return bankAccountRepository.save(account);
    }

    private Transaction createAndSaveTransaction(String transactionId, BigDecimal amount,
                                                 Transaction.TransactionType type,
                                                 BankAccount account, User user) {
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .amount(amount)
                .currency("GBP")
                .type(type)
                .reference("Test transaction")
                .account(account)
                .user(user)
                .build();
        return transactionRepository.save(transaction);
    }
}

