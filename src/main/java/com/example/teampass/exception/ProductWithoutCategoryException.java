package com.example.teampass.exception;

public class ProductWithoutCategoryException extends RuntimeException {
    public ProductWithoutCategoryException(String message) {
        super(message);
    }
}
