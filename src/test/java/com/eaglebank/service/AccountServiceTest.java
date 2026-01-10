package com.eaglebank.service;

import com.eaglebank.domain.Address;
import com.eaglebank.domain.BankAccount;
import com.eaglebank.domain.User;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.UpdateBankAccountRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.ListBankAccountsResponse;
import com.eaglebank.exception.ForbiddenException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.repository.BankAccountRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccountSuccessfully() {
        // Given
        String userId = "usr-abc123";
        CreateBankAccountRequest request = new CreateBankAccountRequest("My Savings", "personal");
        User user = createUser(userId);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(idGenerator.generateAccountNumber()).thenReturn("01234567");
        when(bankAccountRepository.existsByAccountNumber("01234567")).thenReturn(false);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount account = invocation.getArgument(0);
            account.setId(1L);
            account.setCreatedAt(LocalDateTime.now());
            account.setUpdatedAt(LocalDateTime.now());
            return account;
        });

        // When
        BankAccountResponse response = accountService.createAccount(userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountNumber()).isEqualTo("01234567");
        assertThat(response.sortCode()).isEqualTo("10-10-10");
        assertThat(response.name()).isEqualTo("My Savings");
        assertThat(response.accountType()).isEqualTo("personal");
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.currency()).isEqualTo("GBP");
        assertThat(response.createdTimestamp()).isNotNull();
        assertThat(response.updatedTimestamp()).isNotNull();

        verify(userRepository).findByUserId(userId);
        verify(idGenerator).generateAccountNumber();
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundOnCreate() {
        // Given
        String userId = "usr-notfound";
        CreateBankAccountRequest request = new CreateBankAccountRequest("My Savings", "personal");

        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByUserId(userId);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    void shouldListAccountsSuccessfully() {
        // Given
        String userId = "usr-abc123";
        User user = createUser(userId);
        BankAccount account1 = createBankAccount("01234567", "Account 1", user);
        BankAccount account2 = createBankAccount("01234568", "Account 2", user);

        when(bankAccountRepository.findByUserUserId(userId))
                .thenReturn(Arrays.asList(account1, account2));

        // When
        ListBankAccountsResponse response = accountService.listAccounts(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accounts()).hasSize(2);
        assertThat(response.accounts()).extracting(BankAccountResponse::accountNumber)
                .containsExactlyInAnyOrder("01234567", "01234568");
        assertThat(response.accounts()).extracting(BankAccountResponse::name)
                .containsExactlyInAnyOrder("Account 1", "Account 2");

        verify(bankAccountRepository).findByUserUserId(userId);
    }

    @Test
    void shouldGetAccountByAccountNumberSuccessfully() {
        // Given
        String userId = "usr-abc123";
        String accountNumber = "01234567";
        User user = createUser(userId);
        BankAccount account = createBankAccount(accountNumber, "My Account", user);

        when(bankAccountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));

        // When
        BankAccountResponse response = accountService.getAccountByAccountNumber(userId, accountNumber);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountNumber()).isEqualTo(accountNumber);
        assertThat(response.name()).isEqualTo("My Account");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenAccountNotFound() {
        // Given
        String userId = "usr-abc123";
        String accountNumber = "01999999";

        when(bankAccountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountByAccountNumber(userId, accountNumber))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bank account not found");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
    }

    @Test
    void shouldThrowForbiddenExceptionWhenAccessingAnotherUsersAccount() {
        // Given
        String userId = "usr-abc123";
        String accountNumber = "01234567";
        User anotherUser = createUser("usr-xyz789");
        BankAccount account = createBankAccount(accountNumber, "Someone Else's Account", anotherUser);

        when(bankAccountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountByAccountNumber(userId, accountNumber))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not allowed to access");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
    }

    @Test
    void shouldUpdateAccountSuccessfully() {
        // Given
        String userId = "usr-abc123";
        String accountNumber = "01234567";
        UpdateBankAccountRequest request = new UpdateBankAccountRequest("Updated Name", "personal");
        User user = createUser(userId);
        BankAccount account = createBankAccount(accountNumber, "Old Name", user);

        when(bankAccountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BankAccountResponse response = accountService.updateAccount(userId, accountNumber, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Updated Name");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(bankAccountRepository).save(account);
    }

    @Test
    void shouldThrowForbiddenExceptionWhenUpdatingAnotherUsersAccount() {
        // Given
        String userId = "usr-abc123";
        String accountNumber = "01234567";
        UpdateBankAccountRequest request = new UpdateBankAccountRequest("Updated Name", "personal");
        User anotherUser = createUser("usr-xyz789");
        BankAccount account = createBankAccount(accountNumber, "Someone Else's Account", anotherUser);

        when(bankAccountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));

        // When & Then
        assertThatThrownBy(() -> accountService.updateAccount(userId, accountNumber, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not allowed to update");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    void shouldDeleteAccountSuccessfully() {
        // Given
        String userId = "usr-abc123";
        String accountNumber = "01234567";
        User user = createUser(userId);
        BankAccount account = createBankAccount(accountNumber, "My Account", user);

        when(bankAccountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));

        // When
        accountService.deleteAccount(userId, accountNumber);

        // Then
        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(bankAccountRepository).delete(account);
    }

    @Test
    void shouldThrowForbiddenExceptionWhenDeletingAnotherUsersAccount() {
        // Given
        String userId = "usr-abc123";
        String accountNumber = "01234567";
        User anotherUser = createUser("usr-xyz789");
        BankAccount account = createBankAccount(accountNumber, "Someone Else's Account", anotherUser);

        when(bankAccountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));

        // When & Then
        assertThatThrownBy(() -> accountService.deleteAccount(userId, accountNumber))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not allowed to delete");

        verify(bankAccountRepository).findByAccountNumber(accountNumber);
        verify(bankAccountRepository, never()).delete(any(BankAccount.class));
    }

    private User createUser(String userId) {
        return User.builder()
                .id(1L)
                .userId(userId)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .phoneNumber("+447123456789")
                .address(Address.builder()
                        .line1("123 Main St")
                        .town("London")
                        .county("Greater London")
                        .postcode("SW1A 1AA")
                        .build())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private BankAccount createBankAccount(String accountNumber, String name, User user) {
        return BankAccount.builder()
                .id(1L)
                .accountNumber(accountNumber)
                .sortCode("10-10-10")
                .name(name)
                .accountType(BankAccount.AccountType.PERSONAL)
                .balance(BigDecimal.ZERO)
                .currency("GBP")
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

