package com.virtualcompanion.billingservice.entity;

public class UsageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "usage_type", nullable = false)
    private UsageType usageType;
    
    @Column(n = "quantity", nullable = false)
    private Long quantity;
    
    @Column(n = "unit")
    private String unit;
    
    @Column(n = "resource_id")
    private String resourceId;
    
    @Column(n = "description")
    private String description;
    
    @Column(n = "timestamp", nullable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;
    
    public enum UsageType {
        MESSAGE_SENT,
        TOKEN_USED,
        VIDEO_MINUTE,
        VOICE_MINUTE,
        STORAGE_GB,
        API_CALL,
        CHARACTER_CREATED
    }
}
