package com.virtualcompanion.billingservice.entity;

public class PaymentMethod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "stripe_payment_method_id", unique = true)
    private String stripePaymentMethodId;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "method_type", nullable = false)
    private MethodType methodType;
    
    @Column(n = "card_brand")
    private String cardBrand;
    
    @Column(n = "card_last4")
    private String cardLast4;
    
    @Column(n = "card_exp_month")
    private Integer cardExpMonth;
    
    @Column(n = "card_exp_year")
    private Integer cardExpYear;
    
    @Column(n = "billing_email")
    private String billingEmail;
    
    @Column(n = "billing_n")
    private String billingN;
    
    @Column(n = "billing_address", columnDefinition = "JSON")
    private String billingAddress;
    
    @Column(n = "is_default")
    private boolean isDefault = false;
    
    @Column(n = "is_verified")
    private boolean isVerified = false;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum MethodType {
        CARD,
        BANK_ACCOUNT,
        PAYPAL,
        APPLE_PAY,
        GOOGLE_PAY
    }
}
