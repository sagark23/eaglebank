package com.eaglebank.service;

import com.eaglebank.domain.User;
import com.eaglebank.dto.request.LoginRequest;
import com.eaglebank.dto.response.LoginResponse;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        User user = User.builder()
                .userId("usr-test123")
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .build();
        UserResponse userResponse = UserResponse.builder()
                .id("usr-test123")
                .email("john@example.com")
                .name("John Doe")
                .build();

        when(userService.findByEmail(request.email())).thenReturn(user);
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(user.getUserId(), user.getEmail())).thenReturn("jwt-token");
        when(userService.getUserById(user.getUserId())).thenReturn(userResponse);

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo("usr-test123");

        verify(userService).findByEmail(request.email());
        verify(passwordEncoder).matches(request.password(), user.getPasswordHash());
        verify(jwtTokenProvider).generateToken(user.getUserId(), user.getEmail());
        verify(userService).getUserById(user.getUserId());
    }

    @Test
    void shouldThrowUnauthorizedExceptionForInvalidPassword() {
        // Given
        LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");
        User user = User.builder()
                .userId("usr-test123")
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .build();

        when(userService.findByEmail(request.email())).thenReturn(user);
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userService).findByEmail(request.email());
        verify(passwordEncoder).matches(request.password(), user.getPasswordHash());
        verify(jwtTokenProvider, never()).generateToken(anyString(), anyString());
        verify(userService, never()).getUserById(anyString());
    }

    @Test
    void shouldThrowExceptionForNonExistentUser() {
        // Given
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        when(userService.findByEmail(request.email())).thenThrow(
                new com.eaglebank.exception.ResourceNotFoundException("User not found"));

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(com.eaglebank.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userService).findByEmail(request.email());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}

