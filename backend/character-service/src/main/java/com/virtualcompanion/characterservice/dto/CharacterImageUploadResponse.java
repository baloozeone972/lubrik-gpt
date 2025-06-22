package com.virtualcompanion.characterservice.dto;

public class CharacterImageUploadResponse {

    private UUID imageId;
    private String url;
    private String thumbnailUrl;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
}
