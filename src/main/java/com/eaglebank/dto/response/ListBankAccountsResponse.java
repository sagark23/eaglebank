package com.eaglebank.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ListBankAccountsResponse(
        List<BankAccountResponse> accounts
) {
}


