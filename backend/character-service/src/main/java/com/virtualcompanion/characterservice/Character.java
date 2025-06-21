package com.virtualcompanion.characterservice;

public class Character {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 100)
    private String n;
    
    @Column(length = 500)
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String backstory;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CharacterStatus status = CharacterStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CharacterCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessLevel accessLevel = AccessLevel.FREE;
    
    @OneToOne(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private CharacterPersonality personality;
    
    @OneToOne(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private CharacterAppearance appearance;
    
    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterVoice> voices = new ArrayList<>();
    
    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterImage> images = new ArrayList<>();
    
    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterTag> tags = new ArrayList<>();
    
    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterDialogue> sampleDialogues = new ArrayList<>();
    
    @Column(n = "created_by_user_id")
    private UUID createdByUserId;
    
    @Column(n = "is_official")
    private boolean isOfficial = false;
    
    @Column(n = "age_rating")
    private Integer ageRating = 18;
    
    @Column(n = "popularity_score")
    private Double popularityScore = 0.0;
    
    @Column(n = "interaction_count")
    private Long interactionCount = 0L;
    
    @Column(n = "average_rating")
    private Double averageRating = 0.0;
    
    @Column(n = "rating_count")
    private Long ratingCount = 0L;
    
    @ElementCollection
    @CollectionTable(n = "character_languages", 
        joinColumns = @JoinColumn(n = "character_id"))
    @Column(n = "language_code")
    private Set<String> supportedLanguages = new HashSet<>();
    
    @Column(n = "ai_model_id")
    private String aiModelId;
    
    @Column(n = "ai_model_version")
    private String aiModelVersion;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column(n = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(n = "deleted_at")
    private LocalDateTime deletedAt;
}
