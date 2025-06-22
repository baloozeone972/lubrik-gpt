package com.virtualcompanion.billingservice.entity;

public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "plan_type", nullable = false)
    private PlanType planType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;
    
    @Column(n = "stripe_subscription_id", unique = true)
    private String stripeSubscriptionId;
    
    @Column(n = "stripe_customer_id")
    private String stripeCustomerId;
    
    @Column(n = "current_period_start")
    private LocalDateTime currentPeriodStart;
    
    @Column(n = "current_period_end")
    private LocalDateTime currentPeriodEnd;
    
    @Column(n = "trial_start")
    private LocalDateTime trialStart;
    
    @Column(n = "trial_end")
    private LocalDateTime trialEnd;
    
    @Column(n = "cancel_at_period_end")
    private boolean cancelAtPeriodEnd = false;
    
    @Column(n = "canceled_at")
    private LocalDateTime canceledAt;
    
    @Column(n = "expires_at")
    private LocalDateTime expiresAt;
    
    // Billing Details
    @Column(n = "billing_cycle")
    private BillingCycle billingCycle = BillingCycle.MONTHLY;
    
    @Column(n = "amount", precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(n = "currency", length = 3)
    private String currency = "EUR";
    
    @Column(n = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;
    
    @Column(n = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
    @Column(n = "promo_code")
    private String promoCode;
    
    // Usage Limits
    @OneToOne(mappedBy = "subscription", cascade = CascadeType.ALL)
    private SubscriptionLimits limits;
    
    // Payment Method
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "payment_method_id")
    private PaymentMethod defaultPaymentMethod;
    
    // History
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL)
    private List<SubscriptionHistory> history = new ArrayList<>();
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum PlanType {
        FREE,
        STANDARD,
        PREMIUM,
        VIP,
        ENTERPRISE
    }
    
    public enum SubscriptionStatus {
        ACTIVE,
        TRIALING,
        PAST_DUE,
        CANCELED,
        INCOMPLETE,
        INCOMPLETE_EXPIRED,
        UNPAID,
        PAUSED
    }
    
    public enum BillingCycle {
        MONTHLY,
        QUARTERLY,
        YEARLY
    }
}
