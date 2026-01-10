package com.eaglebank.service;

import com.eaglebank.domain.User;
import com.eaglebank.dto.request.LoginRequest;
import com.eaglebank.dto.response.LoginResponse;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.exception.UnauthorizedException;
import com.eaglebank.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.email());

        User user = userService.findByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Invalid password for email: {}", request.email());
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail());
        UserResponse userResponse = userService.getUserById(user.getUserId());

        log.info("User logged in successfully: {}", user.getUserId());

        return LoginResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }
}

