package com.virtualcompanion.mediaservice.dto;

public class VoiceGenerationRequest {

    @NotNull(message = "Character ID is required")
    private UUID characterId;

    private UUID conversationId;

    @NotBlank(message = "Text content is required")
    @Size(max = 5000, message = "Text cannot exceed 5000 characters")
    private String text;

    private String provider; // elevenlabs, azure, google

    private String voiceId;

    private VoiceSettings settings;

    private String outputFormat; // mp3, wav, ogg

    private Map<String, Object> metadata;
}
