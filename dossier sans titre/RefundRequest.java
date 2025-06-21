package com.virtualcompanion.billingservice.dto;

public class RefundRequest {
    
    @NotNull(message = "Payment ID is required")
    private UUID paymentId;
    
    @Positive(message = "Amount must be positive")
    private BigDecimal amount; // null for full refund
    
    @NotNull(message = "Reason is required")
    private String reason;
    
    private String internalNotes;
}
