package com.virtualcompanion.userservice.repository;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
