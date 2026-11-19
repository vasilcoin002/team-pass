package com.example.teampass.exception;

public class DuplicateLicenseRequestException extends RuntimeException {
    public DuplicateLicenseRequestException(String message) {
        super(message);
    }
}
