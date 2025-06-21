package com.virtualcompanion.moderationservice.dto;

public class ContentReportResponse {
    
    private UUID reportId;
    private String status; // submitted, under_review, resolved, dismissed
    private String priority; // low, medium, high, critical
    private LocalDateTime submittedAt;
    private LocalDateTime estimatedReviewTime;
    private String referenceNumber;
}
