package com.virtualcompanion.mediaservice.dto;

public class StreamingRequest {

    @NotNull(message = "Character ID is required")
    private UUID characterId;

    private UUID conversationId;

    @NotNull(message = "Stream type is required")
    private String streamType; // video, audio, screen

    private StreamingConfig config;

    private Map<String, Object> metadata;
}
