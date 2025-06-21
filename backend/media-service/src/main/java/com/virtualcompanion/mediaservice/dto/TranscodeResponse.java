package com.virtualcompanion.mediaservice.dto;

public class TranscodeResponse {
    
    private UUID jobId;
    private String status; // queued, processing, completed, failed
    private Integer progress;
    private List<MediaVariant> variants;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}
