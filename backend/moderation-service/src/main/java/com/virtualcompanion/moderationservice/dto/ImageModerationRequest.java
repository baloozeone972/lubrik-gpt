package com.virtualcompanion.moderationservice.dto;

public class ImageModerationRequest {
    
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    private UUID characterId;
    
    private String contentType; // avatar, message_attachment, character_image
    
    private Long fileSize;
    
    private String mimeType;
    
    private Map<String, Object> metadata;
}
