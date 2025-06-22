package com.virtualcompanion.billingservice.entity;

public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "subscription_id")
    private Subscription subscription;
    
    @Column(n = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;
    
    @Column(n = "stripe_charge_id")
    private String stripeChargeId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "payment_type", nullable = false)
    private PaymentType paymentType;
    
    @Column(n = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(n = "currency", length = 3, nullable = false)
    private String currency;
    
    @Column(n = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(n = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(n = "description")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "payment_method_id")
    private PaymentMethod paymentMethod;
    
    @Column(n = "failure_reason")
    private String failureReason;
    
    @Column(n = "refunded_amount", precision = 10, scale = 2)
    private BigDecimal refundedAmount;
    
    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL)
    private Invoice invoice;
    
    @Column(n = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "completed_at")
    private LocalDateTime completedAt;
    
    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }
    
    public enum PaymentType {
        SUBSCRIPTION,
        ONE_TIME,
        TOP_UP,
        REFUND
    }
}
