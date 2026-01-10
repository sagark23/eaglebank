package com.eaglebank.util;

import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Component
public class IdGenerator {

    private final Random random = new Random();

    public String generateUserId() {
        return "usr-" + generateRandomString(12);
    }

    public String generateTransactionId() {
        return "tan-" + generateRandomString(12);
    }

    public String generateAccountNumber() {
        // Generate 6 random digits
        int number = 100000 + random.nextInt(900000);
        return "01" + number;
    }

    private String generateRandomString(int length) {
        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, length);
    }
}

