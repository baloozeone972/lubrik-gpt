package com.virtualcompanion.userservice.dto.request;

public class AccountDeletionRequest {

    @NotBlank(message = "Password is required")
    private String password;

    private String reason;
    private Boolean immediateDelete = false;
}
