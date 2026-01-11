package com.eaglebank.service;

import com.eaglebank.domain.User;
import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.UpdateUserRequest;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.exception.ConflictException;
import com.eaglebank.exception.ResourceNotFoundException;
import com.eaglebank.repository.UserRepository;
import com.eaglebank.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final AccountService accountService;

    public UserResponse createUser(CreateUserRequest request) {
        log.debug("Creating user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists: " + request.email());
        }

        String userId = idGenerator.generateUserId();
        String passwordHash = passwordEncoder.encode(request.password());

        User user = User.builder()
                .userId(userId)
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordHash)
                .phoneNumber(request.phoneNumber())
                .address(request.address().toEntity())
                .build();

        User saved = userRepository.save(user);
        log.info("User created successfully: {}", saved.getUserId());

        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        log.debug("Fetching user with userId: {}", userId);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        log.debug("Updating user with userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Update fields if provided
        if (request.name() != null && !request.name().isBlank()) {
            user.updateName(request.name());
        }

        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            user.updatePhoneNumber(request.phoneNumber());
        }

        if (request.address() != null) {
            user.updateAddress(request.address().toEntity());
        }

        User updated = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return UserResponse.from(updated);
    }

    public void deleteUser(String userId) {
        log.debug("Deleting user with userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (accountService.hasAccounts(userId)) {
            throw new ConflictException("Cannot delete user with existing bank accounts. Please delete all accounts first.");
        }

        userRepository.delete(user);
        log.info("User deleted successfully: {}", userId);
    }
}

