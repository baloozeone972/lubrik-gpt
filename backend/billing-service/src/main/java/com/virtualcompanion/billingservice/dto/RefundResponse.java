package com.virtualcompanion.billingservice.dto;

public class RefundResponse {
    
    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    private String status; // pending, succeeded, failed, canceled
    private String reason;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
