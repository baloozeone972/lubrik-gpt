package com.virtualcompanion.mediaservice.dto;

public class MediaUploadRequest {

    private UUID characterId;

    private UUID conversationId;

    @NotNull(message = "Media type is required")
    private String mediaType; // video, audio, image

    private String title;

    private String description;

    private Boolean isPublic;

    private Map<String, Object> metadata;
}
