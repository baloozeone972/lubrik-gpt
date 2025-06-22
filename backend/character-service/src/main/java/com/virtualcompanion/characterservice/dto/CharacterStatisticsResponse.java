package com.virtualcompanion.characterservice.dto;

public class CharacterStatisticsResponse {

    private Long totalConversations;
    private Long uniqueUsers;
    private Double averageRating;
    private Long totalRatings;
    private Map<Integer, Long> ratingDistribution;
    private Long totalMessages;
    private Double averageConversationLength;
    private Map<String, Long> usageByDay;
    private Map<String, Long> usageByHour;
    private Double popularityScore;
}
