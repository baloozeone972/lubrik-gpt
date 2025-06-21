package com.virtualcompanion.moderationservice.dto;

public class ImageModerationResponse {
    
    private UUID moderationId;
    private String status; // approved, rejected, flagged, review_required
    private Double confidenceScore;
    private List<ViolationDetail> violations;
    private Map<String, DetectionResult> detections;
    private Boolean requiresBlur;
    private List<BoundingBox> sensitiveAreas;
    private LocalDateTime timestamp;
}
