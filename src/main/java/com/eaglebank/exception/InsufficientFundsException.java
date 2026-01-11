package com.eaglebank.exception;

/**
 * Domain exception thrown when attempting to withdraw more than the available balance.
 * This is thrown by domain methods to enforce invariants.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

