package com.eaglebank.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ListTransactionsResponse(
        List<TransactionResponse> transactions
) {
}

