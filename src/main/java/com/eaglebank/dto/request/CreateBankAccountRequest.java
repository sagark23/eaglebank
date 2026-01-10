package com.eaglebank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record CreateBankAccountRequest(
        @NotBlank(message = "Account name is required")
        String name,

        @NotNull(message = "Account type is required")
        @Pattern(regexp = "personal", message = "Account type must be 'personal'")
        String accountType
) {
}


