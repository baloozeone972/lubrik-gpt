package com.virtualcompanion.characterservice;

public class UserCharacter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "character_id", nullable = false)
    private Character character;
    
    @Column(n = "is_favorite")
    private boolean isFavorite = false;
    
    @Column(n = "is_unlocked")
    private boolean isUnlocked = true;
    
    @Column(n = "unlock_method")
    private String unlockMethod;
    
    @Column(n = "relationship_level")
    private Integer relationshipLevel = 0;
    
    @Column(n = "interaction_count")
    private Long interactionCount = 0L;
    
    @Column(n = "last_interaction_at")
    private LocalDateTime lastInteractionAt;
    
    @Column(n = "custom_n")
    private String customN;
    
    @Column(n = "memory_enabled")
    private boolean memoryEnabled = true;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
