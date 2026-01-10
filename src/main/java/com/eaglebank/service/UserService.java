package com.eaglebank.service;

import com.eaglebank.domain.User;
import com.eaglebank.dto.request.CreateUserRequest;
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

    public UserResponse createUser(CreateUserRequest request) {
        log.debug("Creating user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists: " + request.email());
        }

        String userId = idGenerator.generateUserId();

        // Generate a default password hash for now (in real system, would be set via separate endpoint)
        String passwordHash = passwordEncoder.encode("temporary-password-" + userId);

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
}

