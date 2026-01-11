package com.eaglebank.dto.request;

import jakarta.validation.Valid;

public record UpdateUserRequest(
        String name,
        String phoneNumber,
        @Valid
        AddressRequest address
) {
}

