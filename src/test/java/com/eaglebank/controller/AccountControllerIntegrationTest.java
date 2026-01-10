package com.eaglebank.controller;

import com.eaglebank.dto.request.AddressRequest;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.UpdateBankAccountRequest;
import com.eaglebank.dto.response.BankAccountResponse;
import com.eaglebank.dto.response.LoginResponse;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.service.AccountService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    private UserResponse testUser;
    private String authToken;
    private BankAccountResponse testAccount;

    @BeforeEach
    void setUp() throws Exception {
        // Create test user
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .name("Test User")
                .email("testaccount@example.com")
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
                    "email": "testaccount@example.com",
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
    void shouldCreateAccountSuccessfully() throws Exception {
        // Given
        CreateBankAccountRequest request = new CreateBankAccountRequest("My Savings", "personal");

        // When & Then
        mockMvc.perform(post("/v1/accounts")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Savings"))
                .andExpect(jsonPath("$.accountType").value("personal"))
                .andExpect(jsonPath("$.sortCode").value("10-10-10"))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.accountNumber").isNotEmpty())
                .andExpect(jsonPath("$.createdTimestamp").isNotEmpty())
                .andExpect(jsonPath("$.updatedTimestamp").isNotEmpty());
    }

    @Test
    void shouldReturn400WhenCreatingAccountWithoutName() throws Exception {
        // Given
        String requestJson = """
                {
                    "accountType": "personal"
                }
                """;

        // When & Then
        mockMvc.perform(post("/v1/accounts")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenCreatingAccountWithoutAuth() throws Exception {
        // Given
        CreateBankAccountRequest request = new CreateBankAccountRequest("My Savings", "personal");

        // When & Then
        mockMvc.perform(post("/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListAccountsSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/accounts")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts", hasSize(1)))
                .andExpect(jsonPath("$.accounts[0].accountNumber").value(testAccount.accountNumber()))
                .andExpect(jsonPath("$.accounts[0].name").value("Test Account"));
    }

    @Test
    void shouldReturn401WhenListingAccountsWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGetAccountByAccountNumberSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/accounts/" + testAccount.accountNumber())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(testAccount.accountNumber()))
                .andExpect(jsonPath("$.name").value("Test Account"))
                .andExpect(jsonPath("$.accountType").value("personal"))
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void shouldReturn404WhenAccountNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/accounts/01999999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenAccessingAnotherUsersAccount() throws Exception {
        // Create another user and their account
        CreateUserRequest anotherUserRequest = CreateUserRequest.builder()
                .name("Another User")
                .email("another@example.com")
                .password("password123")
                .phoneNumber("+447987654321")
                .address(AddressRequest.builder()
                        .line1("456 Oak St")
                        .town("Manchester")
                        .county("Greater Manchester")
                        .postcode("M1 1AA")
                        .build())
                .build();
        UserResponse anotherUser = userService.createUser(anotherUserRequest);

        CreateBankAccountRequest accountRequest = new CreateBankAccountRequest("Another Account", "personal");
        BankAccountResponse anotherAccount = accountService.createAccount(anotherUser.id(), accountRequest);

        // When & Then - current user tries to access another user's account
        mockMvc.perform(get("/v1/accounts/" + anotherAccount.accountNumber())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateAccountSuccessfully() throws Exception {
        // Given
        UpdateBankAccountRequest request = new UpdateBankAccountRequest("Updated Account Name", "personal");

        // When & Then
        mockMvc.perform(patch("/v1/accounts/" + testAccount.accountNumber())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(testAccount.accountNumber()))
                .andExpect(jsonPath("$.name").value("Updated Account Name"))
                .andExpect(jsonPath("$.accountType").value("personal"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentAccount() throws Exception {
        // Given
        UpdateBankAccountRequest request = new UpdateBankAccountRequest("Updated Name", "personal");

        // When & Then
        mockMvc.perform(patch("/v1/accounts/01999999")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenUpdatingAnotherUsersAccount() throws Exception {
        // Create another user and their account
        CreateUserRequest anotherUserRequest = CreateUserRequest.builder()
                .name("Another User")
                .email("another2@example.com")
                .password("password123")
                .phoneNumber("+447987654321")
                .address(AddressRequest.builder()
                        .line1("456 Oak St")
                        .town("Manchester")
                        .county("Greater Manchester")
                        .postcode("M1 1AA")
                        .build())
                .build();
        UserResponse anotherUser = userService.createUser(anotherUserRequest);

        CreateBankAccountRequest accountRequest = new CreateBankAccountRequest("Another Account", "personal");
        BankAccountResponse anotherAccount = accountService.createAccount(anotherUser.id(), accountRequest);

        // Given
        UpdateBankAccountRequest request = new UpdateBankAccountRequest("Malicious Update", "personal");

        // When & Then - current user tries to update another user's account
        mockMvc.perform(patch("/v1/accounts/" + anotherAccount.accountNumber())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteAccountSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(delete("/v1/accounts/" + testAccount.accountNumber())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify account is deleted
        mockMvc.perform(get("/v1/accounts/" + testAccount.accountNumber())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentAccount() throws Exception {
        // When & Then
        mockMvc.perform(delete("/v1/accounts/01999999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenDeletingAnotherUsersAccount() throws Exception {
        // Create another user and their account
        CreateUserRequest anotherUserRequest = CreateUserRequest.builder()
                .name("Another User")
                .email("another3@example.com")
                .password("password123")
                .phoneNumber("+447987654321")
                .address(AddressRequest.builder()
                        .line1("456 Oak St")
                        .town("Manchester")
                        .county("Greater Manchester")
                        .postcode("M1 1AA")
                        .build())
                .build();
        UserResponse anotherUser = userService.createUser(anotherUserRequest);

        CreateBankAccountRequest accountRequest = new CreateBankAccountRequest("Another Account", "personal");
        BankAccountResponse anotherAccount = accountService.createAccount(anotherUser.id(), accountRequest);

        // When & Then - current user tries to delete another user's account
        mockMvc.perform(delete("/v1/accounts/" + anotherAccount.accountNumber())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }
}

