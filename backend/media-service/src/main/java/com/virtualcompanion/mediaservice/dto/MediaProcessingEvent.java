package com.virtualcompanion.mediaservice.dto;

public class MediaProcessingEvent {

    private String eventType; // upload_started, processing, completed, failed
    private UUID mediaId;
    private UUID userId;
    private String processingType;
    private Integer progress;
    private String status;
    private String message;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}
