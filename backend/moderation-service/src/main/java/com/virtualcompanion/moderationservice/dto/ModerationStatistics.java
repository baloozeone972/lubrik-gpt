package com.virtualcompanion.moderationservice.dto;

public class ModerationStatistics {

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Long totalContentReviewed;
    private Long automatedDecisions;
    private Long humanDecisions;
    private Map<String, Long> violationsByCategory;
    private Map<String, Long> actionsTaken;
    private Double averageResponseTime;
    private Double automationRate;
    private Long appealsReceived;
    private Long appealsOverturned;
    private Map<String, Object> additionalMetrics;
}
