package com.virtualcompanion.moderationservice.dto;

public class TextModerationResponse {
    
    private UUID moderationId;
    private String status; // approved, rejected, flagged, review_required
    private Double confidenceScore;
    private List<ViolationDetail> violations;
    private Map<String, Double> categoryScores;
    private String moderatedText; // if content was filtered
    private List<String> flaggedPhrases;
    private String reviewReason;
    private LocalDateTime timestamp;
}
