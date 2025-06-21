package com.virtualcompanion.moderationservice.dto;

public class AppealResponse {
    
    private UUID appealId;
    private String status; // submitted, under_review, upheld, overturned
    private LocalDateTime submittedAt;
    private LocalDateTime reviewDeadline;
    private String referenceNumber;
}
