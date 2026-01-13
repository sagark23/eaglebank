package com.eaglebank.controller;

import com.eaglebank.dto.request.CreateUserRequest;
import com.eaglebank.dto.request.UpdateUserRequest;
import com.eaglebank.dto.response.UserResponse;
import com.eaglebank.security.CustomUserDetails;
import com.eaglebank.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Create user request for email: {}", request.email());
        return userService.createUser(request);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@securityService.isOwner(#userId)")
    public UserResponse getUserById(
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Get user request for userId: {} by user: {}", userId, currentUser.getUserId());
        return userService.getUserById(userId);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("@securityService.isOwner(#userId)")
    public UserResponse updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Update user request for userId: {} by user: {}", userId, currentUser.getUserId());
        return userService.updateUser(userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@securityService.isOwner(#userId)")
    public void deleteUser(
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Delete user request for userId: {} by user: {}", userId, currentUser.getUserId());

        userService.deleteUser(userId);
    }
}

