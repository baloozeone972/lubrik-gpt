package com.virtualcompanion.billingservice.dto;

public class CreateSubscriptionRequest {

    @NotNull(message = "Plan is required")
    private String plan; // standard, premium, vip

    private String paymentMethodId; // Stripe payment method ID

    private String paymentProvider; // stripe, paypal

    private Boolean startTrial;

    private Map<String, Object> metadata;
}
