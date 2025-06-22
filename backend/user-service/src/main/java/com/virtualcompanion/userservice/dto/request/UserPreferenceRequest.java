package com.virtualcompanion.userservice.dto.request;

public class UserPreferenceRequest {

    @NotBlank(message = "Preference key is required")
    private String key;

    private String value;
    private String type;
}
