package com.virtualcompanion.billingservice.dto;

public class InvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private UUID userId;
    private UUID subscriptionId;
    private String status; // draft, open, paid, void, uncollectible
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private String currency;
    private LocalDateTime invoiceDate;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private String paymentProvider;
    private String externalInvoiceId;
    private String downloadUrl;
    private List<InvoiceLineItem> lineItems;
}
