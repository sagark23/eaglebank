package com.eaglebank.repository;

import com.eaglebank.config.JpaConfig;
import com.eaglebank.domain.Address;
import com.eaglebank.domain.BankAccount;
import com.eaglebank.domain.User;
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
class BankAccountRepositoryTest {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void shouldSaveAndFindAccountByAccountNumber() {
        // Given
        User user = createAndSaveUser("usr-test123", "test@example.com");
        BankAccount account = BankAccount.builder()
            .accountNumber("01234567")
            .sortCode("10-10-10")
            .name("Personal Account")
            .accountType(BankAccount.AccountType.PERSONAL)
            .balance(BigDecimal.ZERO)
            .currency("GBP")
            .user(user)
            .build();

        // When
        BankAccount saved = bankAccountRepository.save(account);
        Optional<BankAccount> found = bankAccountRepository.findByAccountNumber("01234567");

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Personal Account");
        assertThat(found.get().getAccountType()).isEqualTo(BankAccount.AccountType.PERSONAL);
        assertThat(found.get().getSortCode()).isEqualTo("10-10-10");
        assertThat(found.get().getCurrency()).isEqualTo("GBP");
        assertThat(found.get().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldFindAccountsByUserId() {
        // Given
        User user = createAndSaveUser("usr-test456", "john@example.com");
        BankAccount account1 = createBankAccount("01111111", "Account 1", user);
        BankAccount account2 = createBankAccount("01222222", "Account 2", user);
        bankAccountRepository.save(account1);
        bankAccountRepository.save(account2);

        // When
        List<BankAccount> accounts = bankAccountRepository.findByUserUserId("usr-test456");

        // Then
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(BankAccount::getName)
            .containsExactlyInAnyOrder("Account 1", "Account 2");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoAccounts() {
        // Given
        createAndSaveUser("usr-noaccounts", "noaccounts@example.com");

        // When
        List<BankAccount> accounts = bankAccountRepository.findByUserUserId("usr-noaccounts");

        // Then
        assertThat(accounts).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenAccountNumberExists() {
        // Given
        User user = createAndSaveUser("usr-test789", "exists@example.com");
        BankAccount account = createBankAccount("01333333", "Test Account", user);
        bankAccountRepository.save(account);

        // When
        boolean exists = bankAccountRepository.existsByAccountNumber("01333333");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenAccountNumberDoesNotExist() {
        // When
        boolean exists = bankAccountRepository.existsByAccountNumber("01999999");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldCountAccountsByUserId() {
        // Given
        User user = createAndSaveUser("usr-count", "count@example.com");
        bankAccountRepository.save(createBankAccount("01444444", "Account 1", user));
        bankAccountRepository.save(createBankAccount("01555555", "Account 2", user));
        bankAccountRepository.save(createBankAccount("01666666", "Account 3", user));

        // When
        long count = bankAccountRepository.countByUserUserId("usr-count");

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldReturnZeroCountForUserWithNoAccounts() {
        // Given
        createAndSaveUser("usr-nocount", "nocount@example.com");

        // When
        long count = bankAccountRepository.countByUserUserId("usr-nocount");

        // Then
        assertThat(count).isZero();
    }

    @Test
    void shouldUpdateAccountBalance() {
        // Given
        User user = createAndSaveUser("usr-balance", "balance@example.com");
        BankAccount account = createBankAccount("01777777", "Balance Test", user);
        bankAccountRepository.save(account);

        // When
        account.setBalance(new BigDecimal("1500.50"));
        BankAccount updated = bankAccountRepository.save(account);

        // Then
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("1500.50"));

        // Verify in database
        Optional<BankAccount> found = bankAccountRepository.findByAccountNumber("01777777");
        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo(new BigDecimal("1500.50"));
    }

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

    private BankAccount createBankAccount(String accountNumber, String name, User user) {
        return BankAccount.builder()
            .accountNumber(accountNumber)
            .sortCode("10-10-10")
            .name(name)
            .accountType(BankAccount.AccountType.PERSONAL)
            .balance(BigDecimal.ZERO)
            .currency("GBP")
            .user(user)
            .build();
    }
}

