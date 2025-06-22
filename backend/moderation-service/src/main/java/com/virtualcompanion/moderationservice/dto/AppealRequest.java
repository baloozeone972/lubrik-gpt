package com.virtualcompanion.moderationservice.dto;

public class AppealRequest {

    @NotNull(message = "Decision ID is required")
    private UUID decisionId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Appeal reason is required")
    @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
    private String reason;

    private Map<String, Object> additionalInfo;
}
