package com.virtualcompanion.userservice.dto.response;

public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp = LocalDateTime.now();
    private Map<String, String> validationErrors;
}
