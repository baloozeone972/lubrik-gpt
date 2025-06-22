package com.virtualcompanion.billingservice.entity;

public class SubscriptionLimits {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "subscription_id", nullable = false)
    private Subscription subscription;
    
    // Character Limits
    @Column(n = "max_characters")
    private Integer maxCharacters;
    
    @Column(n = "max_custom_characters")
    private Integer maxCustomCharacters;
    
    // Message Limits
    @Column(n = "messages_per_hour")
    private Integer messagesPerHour;
    
    @Column(n = "messages_per_day")
    private Integer messagesPerDay;
    
    @Column(n = "messages_per_month")
    private Integer messagesPerMonth;
    
    // Token Limits
    @Column(n = "tokens_per_hour")
    private Long tokensPerHour;
    
    @Column(n = "tokens_per_day")
    private Long tokensPerDay;
    
    @Column(n = "tokens_per_month")
    private Long tokensPerMonth;
    
    // Feature Access
    @Column(n = "video_chat_enabled")
    private boolean videoChatEnabled;
    
    @Column(n = "voice_chat_enabled")
    private boolean voiceChatEnabled;
    
    @Column(n = "premium_voices_enabled")
    private boolean premiumVoicesEnabled;
    
    @Column(n = "custom_avatars_enabled")
    private boolean customAvatarsEnabled;
    
    @Column(n = "api_access_enabled")
    private boolean apiAccessEnabled;
    
    // Storage Limits
    @Column(n = "max_storage_gb")
    private Integer maxStorageGb;
    
    @Column(n = "max_conversation_history_days")
    private Integer maxConversationHistoryDays;
}
