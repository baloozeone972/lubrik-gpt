package com.virtualcompanion.billingservice.dto;

public class WebhookPayload {
    
    private String provider; // stripe, paypal
    private String eventType;
    private String eventId;
    private Map<String, Object> data;
    private String signature;
}
