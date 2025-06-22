package com.virtualcompanion.billingservice.entity;

public class InvoiceLineItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "invoice_id", nullable = false)
    private Invoice invoice;
    
    @Column(n = "description", nullable = false)
    private String description;
    
    @Column(n = "quantity")
    private Integer quantity = 1;
    
    @Column(n = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(n = "amount", precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(n = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(n = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(n = "metadata", columnDefinition = "JSON")
    private String metadata;
}
