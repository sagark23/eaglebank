package com.eaglebank.dto.response;

import lombok.Builder;

@Builder
public record LoginResponse(
        String token,
        UserResponse user
) {
}
