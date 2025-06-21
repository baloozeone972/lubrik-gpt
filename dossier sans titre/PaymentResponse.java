package com.virtualcompanion.billingservice.dto;

public class PaymentResponse {
    
    private UUID id;
    private UUID userId;
    private UUID subscriptionId;
    private BigDecimal amount;
    private String currency;
    private String status; // pending, processing, succeeded, failed, refunded
    private String paymentProvider;
    private String externalPaymentId;
    private String paymentMethodType;
    private String paymentMethodLast4;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Map<String, Object> metadata;
}
