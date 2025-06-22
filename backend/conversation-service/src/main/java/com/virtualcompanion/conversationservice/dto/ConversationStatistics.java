package com.virtualcompanion.conversationservice.dto;

public class ConversationStatistics {

    private Long totalMessages;
    private Double averageMessagesPerConversation;
    private Double averageConversationDuration;
    private Map<String, Long> messagesByType;
    private Map<String, Long> conversationsByMode;
    private Map<String, Double> emotionDistribution;
}
