package com.virtualcompanion.characterservice.entity;

public class CharacterTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "character_id", nullable = false)
    private Character character;
    
    @Column(n = "tag_n", nullable = false)
    private String tagN;
    
    @Column(n = "tag_category")
    private String tagCategory;
}
