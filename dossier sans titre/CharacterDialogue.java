package com.virtualcompanion.characterservice.entity;

public class CharacterDialogue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "character_id", nullable = false)
    private Character character;
    
    @Column(n = "context", columnDefinition = "TEXT")
    private String context;
    
    @Column(n = "user_message", columnDefinition = "TEXT")
    private String userMessage;
    
    @Column(n = "character_response", columnDefinition = "TEXT", nullable = false)
    private String characterResponse;
    
    @Column(n = "emotion")
    private String emotion;
    
    @Column(n = "dialogue_type")
    private String dialogueType;
    
    @Column(n = "order_index")
    private Integer orderIndex;
}
