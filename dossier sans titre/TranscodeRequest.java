package com.virtualcompanion.mediaservice.dto;

public class TranscodeRequest {
    
    @NotNull(message = "Quality preset is required")
    private String qualityPreset; // low, medium, high, custom
    
    private String format; // mp4, webm, etc.
    
    private TranscodeSettings customSettings;
    
    private List<String> additionalFormats;
    
    private Boolean generateThumbnails;
    
    private Integer thumbnailCount;
}
