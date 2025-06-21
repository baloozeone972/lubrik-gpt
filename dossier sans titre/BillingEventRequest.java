package com.virtualcompanion.billingservice.dto;

public class BillingEventRequest {
    
    private String eventType;
    private UUID userId;
    private UUID resourceId;
    private String resourceType;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}
