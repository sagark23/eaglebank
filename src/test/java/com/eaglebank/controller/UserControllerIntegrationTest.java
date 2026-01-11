package com.eaglebank.controller;

import com.eaglebank.dto.request.AddressRequest;
import com.eaglebank.dto.request.CreateBankAccountRequest;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.UpdateUserRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

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

    @BeforeEach
    void setUp() throws Exception {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password123")
                .phoneNumber("+447123456789")
                .address(AddressRequest.builder()
                        .line1("123 Main St")
                        .town("London")
                        .county("Greater London")
                        .postcode("SW1A 1AA")
                        .build())
                .build();
        testUser = userService.createUser(createRequest);

        // Login to get auth token
        String loginJson = """
                {
                    "email": "test@example.com",
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
    }

    @Test
    void shouldCreateUserSuccessfully() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .name("New User")
                .email("newuser@example.com")
                .password("SecurePass123")
                .phoneNumber("+447987654321")
                .address(AddressRequest.builder()
                        .line1("456 Oak St")
                        .town("Manchester")
                        .county("Greater Manchester")
                        .postcode("M1 1AA")
                        .build())
                .build();

        // When & Then
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.name").value("New User"));
    }

    @Test
    void shouldReturn400WhenCreatingUserWithInvalidEmail() throws Exception {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .name("New User")
                .email("invalid-email")
                .password("SecurePass123")
                .phoneNumber("+447987654321")
                .address(AddressRequest.builder()
                        .line1("456 Oak St")
                        .town("Manchester")
                        .county("Greater Manchester")
                        .postcode("M1 1AA")
                        .build())
                .build();

        // When & Then
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenCreatingUserWithDuplicateEmail() throws Exception {
        // Given - user with this email already exists
        CreateUserRequest request = CreateUserRequest.builder()
                .name("Duplicate User")
                .email("test@example.com")
                .password("SecurePass123")
                .phoneNumber("+447987654321")
                .address(AddressRequest.builder()
                        .line1("456 Oak St")
                        .town("Manchester")
                        .county("Greater Manchester")
                        .postcode("M1 1AA")
                        .build())
                .build();

        // When & Then
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldGetUserByIdSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/users/" + testUser.id())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.id()))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+447123456789"));
    }

    @Test
    void shouldReturn401WhenGettingUserWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/users/" + testUser.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenGettingAnotherUsersDetails() throws Exception {
        // Given - create another user
        CreateUserRequest createRequest = CreateUserRequest.builder()
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
        UserResponse anotherUser = userService.createUser(createRequest);

        // When & Then - try to access another user's details
        mockMvc.perform(get("/v1/users/" + anotherUser.id())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenGettingNonExistentUser() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/users/usr-nonexistent")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateUserSuccessfully() throws Exception {
        // Given
        UpdateUserRequest request = new UpdateUserRequest(
                "Updated Name",
                "+447999888777",
                new AddressRequest(
                        "789 New Street",
                        "Suite 100",
                        null,
                        "Birmingham",
                        "West Midlands",
                        "B1 1AA"
                )
        );

        // When & Then
        mockMvc.perform(patch("/v1/users/" + testUser.id())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.id()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.phoneNumber").value("+447999888777"))
                .andExpect(jsonPath("$.address.line1").value("789 New Street"))
                .andExpect(jsonPath("$.address.town").value("Birmingham"));
    }

    @Test
    void shouldUpdateUserWithPartialData() throws Exception {
        // Given - only updating name
        UpdateUserRequest request = new UpdateUserRequest(
                "Only Name Updated",
                null,
                null
        );

        // When & Then
        mockMvc.perform(patch("/v1/users/" + testUser.id())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Only Name Updated"))
                .andExpect(jsonPath("$.phoneNumber").value("+447123456789")) // Original value
                .andExpect(jsonPath("$.email").value("test@example.com")); // Unchanged
    }

    @Test
    void shouldReturn401WhenUpdatingUserWithoutAuth() throws Exception {
        // Given
        UpdateUserRequest request = new UpdateUserRequest("Updated Name", null, null);

        // When & Then
        mockMvc.perform(patch("/v1/users/" + testUser.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenUpdatingAnotherUsersDetails() throws Exception {
        // Given - create another user
        CreateUserRequest createRequest = CreateUserRequest.builder()
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
        UserResponse anotherUser = userService.createUser(createRequest);

        UpdateUserRequest request = new UpdateUserRequest("Hacked Name", null, null);

        // When & Then - try to update another user's details
        mockMvc.perform(patch("/v1/users/" + anotherUser.id())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenUpdatingNonExistentUser() throws Exception {
        // Given
        UpdateUserRequest request = new UpdateUserRequest("Updated Name", null, null);

        // When & Then
        mockMvc.perform(patch("/v1/users/usr-nonexistent")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteUserSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(delete("/v1/users/" + testUser.id())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn409WhenDeletingUserWithAccounts() throws Exception {
        // Given - create a bank account for the user
        CreateBankAccountRequest accountRequest = new CreateBankAccountRequest("My Account", "personal");
        BankAccountResponse account = accountService.createAccount(testUser.id(), accountRequest);

        // When & Then - try to delete user with accounts
        mockMvc.perform(delete("/v1/users/" + testUser.id())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn401WhenDeletingUserWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(delete("/v1/users/" + testUser.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenDeletingAnotherUser() throws Exception {
        // Given - create another user
        CreateUserRequest createRequest = CreateUserRequest.builder()
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
        UserResponse anotherUser = userService.createUser(createRequest);

        // When & Then - try to delete another user
        mockMvc.perform(delete("/v1/users/" + anotherUser.id())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenDeletingNonExistentUser() throws Exception {
        // When & Then
        mockMvc.perform(delete("/v1/users/usr-nonexistent")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }
}

