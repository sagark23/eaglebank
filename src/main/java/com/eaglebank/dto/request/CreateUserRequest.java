package com.eaglebank.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateUserRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Phone number is required")
        @Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
        String phoneNumber,

        @NotNull(message = "Address is required")
        @Valid
        AddressRequest address
) {
}

