package com.eaglebank.controller;

import com.eaglebank.dto.request.AddressRequest;
import com.eaglebank.dto.request.CreateUserRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private UserResponse testUser;

    @BeforeEach
    void setUp() {
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
}

