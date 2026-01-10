package com.eaglebank.dto.response;

import com.eaglebank.domain.BankAccount;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record BankAccountResponse(
        String accountNumber,
        String sortCode,
        String name,
        String accountType,  // lowercase: "personal"
        BigDecimal balance,
        String currency,
        LocalDateTime createdTimestamp,
        LocalDateTime updatedTimestamp
) {
    public static BankAccountResponse from(BankAccount account) {
        if (account == null) {
            return null;
        }
        return new BankAccountResponse(
                account.getAccountNumber(),
                account.getSortCode(),
                account.getName(),
                account.getAccountType().name().toLowerCase(),
                account.getBalance(),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}


