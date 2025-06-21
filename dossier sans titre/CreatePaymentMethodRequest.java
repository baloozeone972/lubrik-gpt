package com.virtualcompanion.billingservice.dto;

public class CreatePaymentMethodRequest {
    
    @NotNull(message = "Payment provider is required")
    private String paymentProvider; // stripe, paypal
    
    private String paymentMethodId; // For Stripe
    
    private String paypalEmail; // For PayPal
    
    private Boolean setAsDefault;
}
