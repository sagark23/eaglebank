package com.eaglebank.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    private final IdGenerator idGenerator = new IdGenerator();

    @Test
    void shouldGenerateUserIdWithCorrectPrefix() {
        // When
        String userId = idGenerator.generateUserId();

        // Then
        assertThat(userId).startsWith("usr-");
        assertThat(userId).hasSize(16); // usr- (4) + 12 chars = 16
    }

    @Test
    void shouldGenerateUniqueUserIds() {
        // When
        String userId1 = idGenerator.generateUserId();
        String userId2 = idGenerator.generateUserId();

        // Then
        assertThat(userId1).isNotEqualTo(userId2);
    }

    @Test
    void shouldGenerateTransactionIdWithCorrectPrefix() {
        // When
        String transactionId = idGenerator.generateTransactionId();

        // Then
        assertThat(transactionId).startsWith("tan-");
        assertThat(transactionId).hasSize(16); // tan- (4) + 12 chars = 16
    }

    @Test
    void shouldGenerateUniqueTransactionIds() {
        // When
        String transactionId1 = idGenerator.generateTransactionId();
        String transactionId2 = idGenerator.generateTransactionId();

        // Then
        assertThat(transactionId1).isNotEqualTo(transactionId2);
    }

    @Test
    void shouldGenerateUserIdWithAlphanumericCharacters() {
        // When
        String userId = idGenerator.generateUserId();
        String idPart = userId.substring(4); // Remove "usr-" prefix

        // Then
        assertThat(idPart).matches("[a-f0-9]{12}");
    }

    @Test
    void shouldGenerateTransactionIdWithAlphanumericCharacters() {
        // When
        String transactionId = idGenerator.generateTransactionId();
        String idPart = transactionId.substring(4); // Remove "tan-" prefix

        // Then
        assertThat(idPart).matches("[a-f0-9]{12}");
    }

    @Test
    void shouldGenerateAccountNumberWithCorrectFormat() {
        // When
        String accountNumber = idGenerator.generateAccountNumber();

        // Then
        assertThat(accountNumber).startsWith("01");
        assertThat(accountNumber).hasSize(8); // 01 + 6 digits
        assertThat(accountNumber).matches("01\\d{6}");
    }

    @Test
    void shouldGenerateUniqueAccountNumbers() {
        // When
        String accountNumber1 = idGenerator.generateAccountNumber();
        String accountNumber2 = idGenerator.generateAccountNumber();

        // Then
        assertThat(accountNumber1).isNotEqualTo(accountNumber2);
    }

    @Test
    void shouldGenerateAccountNumbersInValidRange() {
        // When
        String accountNumber = idGenerator.generateAccountNumber();
        String numberPart = accountNumber.substring(2);
        int number = Integer.parseInt(numberPart);

        // Then
        assertThat(number).isGreaterThanOrEqualTo(100000);
        assertThat(number).isLessThan(1000000);
    }
}

