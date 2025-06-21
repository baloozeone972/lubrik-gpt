package com.virtualcompanion.moderationservice.dto;

public class AgeVerificationResponse {
    
    private UUID verificationId;
    private String status; // verified, failed, pending_review, insufficient_data
    private Integer verifiedAge;
    private String verificationMethod;
    private Double confidenceScore;
    private String failureReason;
    private LocalDateTime verifiedAt;
    private LocalDateTime expiresAt;
}
