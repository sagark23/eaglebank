package com.eaglebank.controller;

import com.eaglebank.dto.request.AddressRequest;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.CreateTransactionRequest;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.LoginResponse;
import com.eaglebank.dto.response.TransactionResponse;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.service.AccountService;
import com.eaglebank.service.TransactionService;
import com.eaglebank.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    private UserResponse testUser;
    private String authToken;
    private BankAccountResponse testAccount;

    @BeforeEach
    void setUp() throws Exception {
        // Create test user
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .name("Test User")
                .email("testtransaction@example.com")
                .password("password123")
                .phoneNumber("+447123456789")
                .address(AddressRequest.builder()
                        .line1("123 Main St")
                        .town("London")
                        .county("Greater London")
                        .postcode("SW1A 1AA")
                        .build())
                .build();
        testUser = userService.createUser(createUserRequest);

        // Login to get auth token
        String loginJson = """
                {
                    "email": "testtransaction@example.com",
                    "password": "password123"
                }
                """;

        String loginResponse = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse loginResponseObj = objectMapper.readValue(loginResponse, LoginResponse.class);
        authToken = loginResponseObj.token();

        // Create a test account
        CreateBankAccountRequest accountRequest = new CreateBankAccountRequest("Test Account", "personal");
        testAccount = accountService.createAccount(testUser.id(), accountRequest);
    }

    @Test
    void shouldCreateDepositSuccessfully() throws Exception {
        // Given
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("100.50"))
                .currency("GBP")
                .type("deposit")
                .reference("Salary payment")
                .build();

        // When & Then
        mockMvc.perform(post("/v1/accounts/" + testAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.type").value("deposit"))
                .andExpect(jsonPath("$.reference").value("Salary payment"))
                .andExpect(jsonPath("$.userId").value(testUser.id()))
                .andExpect(jsonPath("$.createdTimestamp").isNotEmpty());
    }

    @Test
    void shouldCreateWithdrawalSuccessfully() throws Exception {
        // Given - First deposit some money
        CreateTransactionRequest depositRequest = CreateTransactionRequest.builder()
                .amount(new BigDecimal("200.00"))
                .currency("GBP")
                .type("deposit")
                .build();
        transactionService.createTransaction(testAccount.accountNumber(), depositRequest, testUser.id());

        // Create withdrawal request
        CreateTransactionRequest withdrawalRequest = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("withdrawal")
                .reference("ATM withdrawal")
                .build();

        // When & Then
        mockMvc.perform(post("/v1/accounts/" + testAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.type").value("withdrawal"))
                .andExpect(jsonPath("$.reference").value("ATM withdrawal"));
    }

    @Test
    void shouldReturn422WhenWithdrawingWithInsufficientFunds() throws Exception {
        // Given - Account has 0 balance
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("withdrawal")
                .build();

        // When & Then
        mockMvc.perform(post("/v1/accounts/" + testAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Insufficient funds")));
    }

    @Test
    void shouldReturn400WhenCreatingTransactionWithoutAmount() throws Exception {
        // Given
        String requestJson = """
                {
                    "currency": "GBP",
                    "type": "deposit"
                }
                """;

        // When & Then
        mockMvc.perform(post("/v1/accounts/" + testAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenCreatingTransactionWithInvalidType() throws Exception {
        // Given
        String requestJson = """
                {
                    "amount": 100.00,
                    "currency": "GBP",
                    "type": "invalid"
                }
                """;

        // When & Then
        mockMvc.perform(post("/v1/accounts/" + testAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenCreatingTransactionWithoutAuth() throws Exception {
        // Given
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("deposit")
                .build();

        // When & Then
        mockMvc.perform(post("/v1/accounts/" + testAccount.accountNumber() + "/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenCreatingTransactionForAnotherUsersAccount() throws Exception {
        // Given - Create another user and their account
        CreateUserRequest otherUserRequest = CreateUserRequest.builder()
                .name("Other User")
                .email("othertransaction@example.com")
                .password("password123")
                .phoneNumber("+447987654321")
                .address(AddressRequest.builder()
                        .line1("456 Other St")
                        .town("Manchester")
                        .county("Greater Manchester")
                        .postcode("M1 1AA")
                        .build())
                .build();
        UserResponse otherUser = userService.createUser(otherUserRequest);

        CreateBankAccountRequest otherAccountRequest = new CreateBankAccountRequest("Other Account", "personal");
        BankAccountResponse otherAccount = accountService.createAccount(otherUser.id(), otherAccountRequest);

        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("deposit")
                .build();

        // When & Then - Try to create transaction on other user's account
        mockMvc.perform(post("/v1/accounts/" + otherAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenCreatingTransactionForNonExistentAccount() throws Exception {
        // Given
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("deposit")
                .build();

        // When & Then
        mockMvc.perform(post("/v1/accounts/01999999/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListTransactionsSuccessfully() throws Exception {
        // Given - Create some transactions
        CreateTransactionRequest deposit1 = CreateTransactionRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("GBP")
                .type("deposit")
                .reference("First deposit")
                .build();
        transactionService.createTransaction(testAccount.accountNumber(), deposit1, testUser.id());

        CreateTransactionRequest deposit2 = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("deposit")
                .reference("Second deposit")
                .build();
        transactionService.createTransaction(testAccount.accountNumber(), deposit2, testUser.id());

        // When & Then
        mockMvc.perform(get("/v1/accounts/" + testAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions", hasSize(2)));
    }

    @Test
    void shouldReturn403WhenListingTransactionsForAnotherUsersAccount() throws Exception {
        // Given - Create another user and their account
        CreateUserRequest otherUserRequest = CreateUserRequest.builder()
                .name("Other User 2")
                .email("othertransaction2@example.com")
                .password("password123")
                .phoneNumber("+447987654322")
                .address(AddressRequest.builder()
                        .line1("789 Another St")
                        .town("Birmingham")
                        .county("West Midlands")
                        .postcode("B1 1AA")
                        .build())
                .build();
        UserResponse otherUser = userService.createUser(otherUserRequest);

        CreateBankAccountRequest otherAccountRequest = new CreateBankAccountRequest("Other Account 2", "personal");
        BankAccountResponse otherAccount = accountService.createAccount(otherUser.id(), otherAccountRequest);

        // When & Then
        mockMvc.perform(get("/v1/accounts/" + otherAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenListingTransactionsForNonExistentAccount() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/accounts/01999999/transactions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetTransactionSuccessfully() throws Exception {
        // Given - Create a transaction
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("75.00"))
                .currency("GBP")
                .type("deposit")
                .reference("Test transaction")
                .build();
        TransactionResponse transaction = transactionService.createTransaction(
                testAccount.accountNumber(), request, testUser.id());

        // When & Then
        mockMvc.perform(get("/v1/accounts/" + testAccount.accountNumber() +
                        "/transactions/" + transaction.id())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.id()))
                .andExpect(jsonPath("$.amount").value(75.00))
                .andExpect(jsonPath("$.type").value("deposit"))
                .andExpect(jsonPath("$.reference").value("Test transaction"));
    }

    @Test
    void shouldReturn403WhenGettingTransactionForAnotherUsersAccount() throws Exception {
        // Given - Create another user and their account with a transaction
        CreateUserRequest otherUserRequest = CreateUserRequest.builder()
                .name("Other User 3")
                .email("othertransaction3@example.com")
                .password("password123")
                .phoneNumber("+447987654323")
                .address(AddressRequest.builder()
                        .line1("321 Yet Another St")
                        .town("Leeds")
                        .county("West Yorkshire")
                        .postcode("LS1 1AA")
                        .build())
                .build();
        UserResponse otherUser = userService.createUser(otherUserRequest);

        CreateBankAccountRequest otherAccountRequest = new CreateBankAccountRequest("Other Account 3", "personal");
        BankAccountResponse otherAccount = accountService.createAccount(otherUser.id(), otherAccountRequest);

        CreateTransactionRequest transactionRequest = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("deposit")
                .build();
        TransactionResponse otherTransaction = transactionService.createTransaction(
                otherAccount.accountNumber(), transactionRequest, otherUser.id());

        // When & Then
        mockMvc.perform(get("/v1/accounts/" + otherAccount.accountNumber() +
                        "/transactions/" + otherTransaction.id())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenGettingNonExistentTransaction() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/accounts/" + testAccount.accountNumber() +
                        "/transactions/tan-notexist")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenGettingTransactionFromWrongAccount() throws Exception {
        // Given - Create a transaction on testAccount
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("deposit")
                .build();
        TransactionResponse transaction = transactionService.createTransaction(
                testAccount.accountNumber(), request, testUser.id());

        // Create another account for the same user
        CreateBankAccountRequest anotherAccountRequest = new CreateBankAccountRequest("Another Account", "personal");
        BankAccountResponse anotherAccount = accountService.createAccount(testUser.id(), anotherAccountRequest);

        // When & Then - Try to get transaction from different account
        mockMvc.perform(get("/v1/accounts/" + anotherAccount.accountNumber() +
                        "/transactions/" + transaction.id())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateTransactionWithoutReference() throws Exception {
        // Given
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .amount(new BigDecimal("25.00"))
                .currency("GBP")
                .type("deposit")
                .build();

        // When & Then
        mockMvc.perform(post("/v1/accounts/" + testAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(25.00));
    }

    @Test
    void shouldUpdateBalanceCorrectlyAfterMultipleTransactions() throws Exception {
        // Given - Create multiple transactions
        CreateTransactionRequest deposit1 = CreateTransactionRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("GBP")
                .type("deposit")
                .build();
        transactionService.createTransaction(testAccount.accountNumber(), deposit1, testUser.id());

        CreateTransactionRequest deposit2 = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .currency("GBP")
                .type("deposit")
                .build();
        transactionService.createTransaction(testAccount.accountNumber(), deposit2, testUser.id());

        CreateTransactionRequest withdrawal = CreateTransactionRequest.builder()
                .amount(new BigDecimal("30.00"))
                .currency("GBP")
                .type("withdrawal")
                .build();

        // When & Then
        mockMvc.perform(post("/v1/accounts/" + testAccount.accountNumber() + "/transactions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawal)))
                .andExpect(status().isCreated());

        // Verify final balance (should be 120.00 = 100 + 50 - 30)
        mockMvc.perform(get("/v1/accounts/" + testAccount.accountNumber())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(120.00));
    }
}

