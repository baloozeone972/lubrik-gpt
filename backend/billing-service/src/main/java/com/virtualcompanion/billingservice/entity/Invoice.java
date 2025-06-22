package com.virtualcompanion.billingservice.entity;

public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "payment_id")
    private Payment payment;
    
    @Column(n = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;
    
    @Column(n = "stripe_invoice_id", unique = true)
    private String stripeInvoiceId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;
    
    @Column(n = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;
    
    @Column(n = "due_date")
    private LocalDateTime dueDate;
    
    @Column(n = "period_start")
    private LocalDateTime periodStart;
    
    @Column(n = "period_end")
    private LocalDateTime periodEnd;
    
    // Financial Details
    @Column(n = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    @Column(n = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(n = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;
    
    @Column(n = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(n = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(n = "currency", length = 3)
    private String currency;
    
    // Line Items
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();
    
    // Billing Information
    @Column(n = "billing_n")
    private String billingN;
    
    @Column(n = "billing_email")
    private String billingEmail;
    
    @Column(n = "billing_address", columnDefinition = "JSON")
    private String billingAddress;
    
    @Column(n = "tax_id")
    private String taxId;
    
    // Files
    @Column(n = "pdf_url")
    private String pdfUrl;
    
    @Column(n = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum InvoiceStatus {
        DRAFT,
        OPEN,
        PAID,
        VOID,
        UNCOLLECTIBLE
    }
}
