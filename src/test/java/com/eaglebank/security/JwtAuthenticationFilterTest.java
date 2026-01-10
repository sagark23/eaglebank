package com.eaglebank.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWithValidToken() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String userId = "usr-test123";
        String bearerToken = "Bearer " + token;

        UserDetails userDetails = createUserDetails(userId, "test@example.com");

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(userDetailsService.loadUserByUserId(userId)).thenReturn(userDetails);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();

        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getUserIdFromToken(token);
        verify(userDetailsService).loadUserByUserId(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWithInvalidToken() throws ServletException, IOException {
        // Given
        String token = "invalid.jwt.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUserId(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWithoutAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWithMalformedAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("InvalidHeader token");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldHandleExceptionGracefully() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenThrow(new RuntimeException("Token parsing error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private UserDetails createUserDetails(String userId, String email) {
        return new CustomUserDetails(userId, email, "hashedPassword");
    }
}

