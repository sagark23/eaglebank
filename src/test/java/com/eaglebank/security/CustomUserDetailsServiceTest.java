package com.eaglebank.security;

import com.eaglebank.domain.Address;
import com.eaglebank.domain.User;
import com.eaglebank.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldLoadUserByEmail() {
        // Given
        String email = "test@example.com";
        User user = createTestUser("usr-test123", email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo(user.getPasswordHash());
        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        assertThat(customUserDetails.getUserId()).isEqualTo("usr-test123");

        verify(userRepository).findByEmail(email);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundByEmail() {
        // Given
        String email = "notfound@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found with email: " + email);

        verify(userRepository).findByEmail(email);
    }

    @Test
    void shouldLoadUserByUserId() {
        // Given
        String userId = "usr-test456";
        User user = createTestUser(userId, "user@example.com");
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUserId(userId);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(user.getEmail());
        assertThat(userDetails.getPassword()).isEqualTo(user.getPasswordHash());

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        assertThat(customUserDetails.getUserId()).isEqualTo(userId);

        verify(userRepository).findByUserId(userId);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundByUserId() {
        // Given
        String userId = "usr-notfound";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUserId(userId))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found with id: " + userId);

        verify(userRepository).findByUserId(userId);
    }

    @Test
    void customUserDetailsShouldHaveCorrectProperties() {
        // Given
        String userId = "usr-test789";
        User user = createTestUser(userId, "props@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Then
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    private User createTestUser(String userId, String email) {
        return User.builder()
            .id(1L)
            .userId(userId)
            .name("Test User")
            .email(email)
            .passwordHash("hashedPassword123")
            .phoneNumber("+447123456789")
            .address(Address.builder()
                .line1("123 Test St")
                .town("London")
                .county("Greater London")
                .postcode("SW1A 1AA")
                .build())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}

