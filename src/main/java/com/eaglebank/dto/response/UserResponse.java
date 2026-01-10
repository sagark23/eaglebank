package com.eaglebank.dto.response;

import com.eaglebank.domain.User;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserResponse(
        String id,  // userId (usr-xxx)
        String name,
        String email,
        String phoneNumber,
        AddressResponse address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                AddressResponse.from(user.getAddress()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

