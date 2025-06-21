package com.virtualcompanion.moderationservice.dto;

public class TextModerationRequest {
    
    @NotBlank(message = "Text content is required")
    @Size(max = 5000, message = "Text cannot exceed 5000 characters")
    private String text;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    private UUID characterId;
    
    private UUID conversationId;
    
    private String contentType; // message, profile, character_description
    
    private String language;
    
    private Map<String, Object> context;
    
    private Boolean requireHumanReview;
}
