package com.eaglebank.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record UpdateBankAccountRequest(
        String name,

        @Pattern(regexp = "personal", message = "Account type must be 'personal'")
        String accountType
) {
}


