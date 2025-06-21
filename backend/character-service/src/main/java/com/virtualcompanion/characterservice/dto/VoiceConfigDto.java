package com.virtualcompanion.characterservice.dto;

public class VoiceConfigDto {
    
    @NotBlank(message = "Voice provider is required")
    private String provider;
    
    @NotBlank(message = "Voice ID is required")
    private String voiceId;
    
    @Size(max = 100, message = "Voice name cannot exceed 100 characters")
    private String voiceName;
    
    @Size(max = 50, message = "Language cannot exceed 50 characters")
    private String language;
    
    @DecimalMin(value = "0.5", message = "Pitch must be at least 0.5")
    @DecimalMax(value = "2.0", message = "Pitch cannot exceed 2.0")
    private Double pitch;
    
    @DecimalMin(value = "0.5", message = "Speed must be at least 0.5")
    @DecimalMax(value = "2.0", message = "Speed cannot exceed 2.0")
    private Double speed;
    
    @Size(max = 50, message = "Emotion cannot exceed 50 characters")
    private String emotion;
    
    @Size(max = 200, message = "Sample URL cannot exceed 200 characters")
    private String sampleUrl;
}
