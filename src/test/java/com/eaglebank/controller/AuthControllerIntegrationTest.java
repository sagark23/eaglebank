package com.eaglebank.controller;

import com.eaglebank.dto.request.AddressRequest;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.LoginRequest;
import com.eaglebank.dto.response.UserResponse;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    private UserResponse testUser;

    @BeforeEach
    void setUp() {
        // Create a test user
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
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.id").value(testUser.id()))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void shouldReturn401WhenPasswordIsInvalid() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("invalid-email", "password123");

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

