package com.virtualcompanion.billingservice.dto;

public class UsageStatisticsResponse {

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Map<String, UsageDetail> usage;
    private SubscriptionLimits limits;
    private Map<String, Double> usagePercentage;
}
