package com.virtualcompanion.conversationservice;

public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "character_id", nullable = false)
    private UUID characterId;
    
    @Column(n = "title")
    private String title;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationStatus status = ConversationStatus.ACTIVE;
    
    @Column(n = "message_count")
    private Integer messageCount = 0;
    
    @Column(n = "user_message_count")
    private Integer userMessageCount = 0;
    
    @Column(n = "character_message_count")
    private Integer characterMessageCount = 0;
    
    @Column(n = "total_tokens_used")
    private Long totalTokensUsed = 0L;
    
    @Column(n = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    @Column(n = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;
    
    @Column(n = "emotional_state")
    private String emotionalState;
    
    @Column(n = "relationship_score")
    private Integer relationshipScore = 50;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<ConversationMemory> memories = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(n = "conversation_tags", 
        joinColumns = @JoinColumn(n = "conversation_id"))
    @Column(n = "tag")
    private Set<String> tags = new HashSet<>();
    
    @Column(n = "language_code")
    private String languageCode = "en";
    
    @Column(n = "is_favorite")
    private boolean isFavorite = false;
    
    @Column(n = "is_archived")
    private boolean isArchived = false;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
