package com.virtualcompanion.conversationservice;

public class ConversationMemory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @Column(n = "memory_type", nullable = false)
    private String memoryType;
    
    @Column(n = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(n = "embedding_vector", columnDefinition = "vector(1536)")
    private float[] embeddingVector;
    
    @Column(n = "importance_score")
    private Double importanceScore = 0.5;
    
    @Column(n = "emotional_context")
    private String emotionalContext;
    
    @Column(n = "referenced_count")
    private Integer referencedCount = 0;
    
    @Column(n = "last_referenced_at")
    private LocalDateTime lastReferencedAt;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
