package com.eaglebank.dto.response;

import com.eaglebank.domain.Transaction;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionResponse(
        String id,
        BigDecimal amount,
        String currency,
        String type,
        String reference,
        String userId,
        LocalDateTime createdTimestamp
) {
    public static TransactionResponse from(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType().name().toLowerCase(),
                transaction.getReference(),
                transaction.getUser().getUserId(),
                transaction.getCreatedAt()
        );
    }
}

