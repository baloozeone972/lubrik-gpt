package com.virtualcompanion.billingservice.dto;

public class UsageRecordRequest {
    
    @NotNull(message = "Usage type is required")
    private String usageType; // conversation, voice_minutes, video_minutes, storage
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;
    
    private UUID characterId;
    
    private LocalDateTime timestamp;
    
    private Map<String, Object> metadata;
}
