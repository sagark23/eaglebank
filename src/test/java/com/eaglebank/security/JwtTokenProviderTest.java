package com.eaglebank.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "jwt.secret=test-secret-key-must-be-at-least-256-bits-long-for-testing-purposes-with-hs256-algorithm",
    "jwt.expiration=3600000"
})
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldGenerateValidToken() {
        // When
        String token = jwtTokenProvider.generateToken("usr-test123", "test@example.com");

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void shouldExtractUserIdFromToken() {
        // Given
        String userId = "usr-test456";
        String token = jwtTokenProvider.generateToken(userId, "test@example.com");

        // When
        String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void shouldValidateValidToken() {
        // Given
        String token = jwtTokenProvider.generateToken("usr-test789", "test@example.com");

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldHandleTokenWithMultipleClaims() {
        // Given
        String userId = "usr-test999";
        String email = "multi@example.com";
        String token = jwtTokenProvider.generateToken(userId, email);

        // When
        String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
        assertThat(isValid).isTrue();
    }
}

