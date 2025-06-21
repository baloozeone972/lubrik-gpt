package com.virtualcompanion.mediaservice.dto;

public class VoiceGenerationResponse {
    
    private UUID id;
    private String audioUrl;
    private String format;
    private Long fileSize;
    private Double duration;
    private String provider;
    private String voiceId;
    private String status;
    private Double cost;
    private LocalDateTime createdAt;
}
