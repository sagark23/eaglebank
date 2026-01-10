package com.eaglebank.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IdGenerator {

    public String generateUserId() {
        return "usr-" + generateRandomString(12);
    }

    public String generateTransactionId() {
        return "tan-" + generateRandomString(12);
    }

    private String generateRandomString(int length) {
        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, length);
    }
}

