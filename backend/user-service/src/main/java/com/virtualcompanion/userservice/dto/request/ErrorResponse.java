package com.virtualcompanion.userservice.dto.request;

public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp = LocalDateTime.now();
    private Map<String, String> validationErrors;
}
