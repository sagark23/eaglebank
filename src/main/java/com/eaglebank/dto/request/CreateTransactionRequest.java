package com.eaglebank.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateTransactionRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @DecimalMax(value = "10000.00", message = "Amount must not exceed 10000.00")
        @Digits(integer = 5, fraction = 2, message = "Amount must have up to 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        @Pattern(regexp = "GBP", message = "Currency must be 'GBP'")
        String currency,

        @NotNull(message = "Transaction type is required")
        @Pattern(regexp = "deposit|withdrawal", message = "Type must be 'deposit' or 'withdrawal'")
        String type,

        String reference
) {
}

