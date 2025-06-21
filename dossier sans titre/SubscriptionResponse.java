package com.virtualcompanion.billingservice.dto;

public class SubscriptionResponse {
    
    private UUID id;
    private UUID userId;
    private String plan;
    private String status; // active, trialing, past_due, canceled, suspended
    private BigDecimal price;
    private String currency;
    private String billingCycle; // monthly, yearly
    private LocalDateTime startDate;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime trialEndDate;
    private LocalDateTime canceledAt;
    private Boolean autoRenew;
    private String paymentProvider;
    private String externalSubscriptionId;
    private SubscriptionLimits limits;
    private Map<String, Object> metadata;
}
