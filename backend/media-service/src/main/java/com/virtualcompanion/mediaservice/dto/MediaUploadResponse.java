package com.virtualcompanion.mediaservice.dto;

public class MediaUploadResponse {

    private UUID id;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String url;
    private String thumbnailUrl;
    private String processingStatus;
    private Double duration;
    private Integer width;
    private Integer height;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
