package com.example.teampass.exception;

public class InsufficientBudgetException extends RuntimeException {
    public InsufficientBudgetException(String message) {
        super(message);
    }
}
