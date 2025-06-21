package com.virtualcompanion.billingservice.dto;

public class PaymentRequest {
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    private String currency;
    
    private String description;
    
    @NotNull(message = "Payment method is required")
    private String paymentMethodId;
    
    private String paymentProvider; // stripe, paypal
    
    private Map<String, Object> metadata;
}
