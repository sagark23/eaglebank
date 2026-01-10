package com.eaglebank.service;

import com.eaglebank.domain.Address;
import com.eaglebank.domain.User;
import com.eaglebank.dto.request.AddressRequest;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.exception.ConflictException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        CreateUserRequest request = createUserRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(idGenerator.generateUserId()).thenReturn("usr-abc123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("usr-abc123");
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.phoneNumber()).isEqualTo("+447123456789");
        assertThat(response.address()).isNotNull();
        assertThat(response.address().line1()).isEqualTo("123 Main St");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();

        verify(userRepository).existsByEmail(request.email());
        verify(idGenerator).generateUserId();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowConflictExceptionWhenEmailExists() {
        // Given
        CreateUserRequest request = createUserRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldGetUserByIdSuccessfully() {
        // Given
        String userId = "usr-abc123";
        User user = createUser(userId);
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        // When
        UserResponse response = userService.getUserById(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        verify(userRepository).findByUserId(userId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        // Given
        String userId = "usr-notexists";
        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");

        verify(userRepository).findByUserId(userId);
    }

    @Test
    void shouldFindUserByEmail() {
        // Given
        String email = "john@example.com";
        User user = createUser("usr-abc123");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // When
        User found = userService.findByEmail(email);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEmailDoesNotExist() {
        // Given
        String email = "notexists@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findByEmail(email))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");

        verify(userRepository).findByEmail(email);
    }

    private CreateUserRequest createUserRequest() {
        return new CreateUserRequest(
                "John Doe",
                "john@example.com",
                "SecurePass123",
                "+447123456789",
                new AddressRequest(
                        "123 Main St",
                        "Apt 4B",
                        null,
                        "London",
                        "Greater London",
                        "SW1A 1AA"
                )
        );
    }

    private User createUser(String userId) {
        return User.builder()
                .id(1L)
                .userId(userId)
                .name("John Doe")
                .email("john@example.com")
                .passwordHash("hashedPassword")
                .phoneNumber("+447123456789")
                .address(Address.builder()
                        .line1("123 Main St")
                        .line2("Apt 4B")
                        .town("London")
                        .county("Greater London")
                        .postcode("SW1A 1AA")
                        .build())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

