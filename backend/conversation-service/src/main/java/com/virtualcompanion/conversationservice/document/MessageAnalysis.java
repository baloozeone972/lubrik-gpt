package com.virtualcompanion.conversationservice.document;

public class MessageAnalysis {
    private List<String> topics;
    private List<String> entities;
    private Map<String, Double> emotionScores;
    private Double toxicityScore;
    private Boolean requiresModeration;
    private String moderationReason;
    private List<String> suggestedResponses;
}
