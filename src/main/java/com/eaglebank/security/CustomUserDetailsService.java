package com.eaglebank.security;

import com.eaglebank.domain.User;
import com.eaglebank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(
            user.getUserId(),
            user.getEmail(),
            user.getPasswordHash()
        );
    }

    public UserDetails loadUserByUserId(String userId) {
        log.debug("Loading user by userId: {}", userId);

        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        return new CustomUserDetails(
            user.getUserId(),
            user.getEmail(),
            user.getPasswordHash()
        );
    }
}

