package com.virtualcompanion.billingservice.dto;

public class InvoiceLineItem {

    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String type; // subscription, usage, credit, discount
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
