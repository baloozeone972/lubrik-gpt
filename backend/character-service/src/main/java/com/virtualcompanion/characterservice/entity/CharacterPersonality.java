package com.virtualcompanion.characterservice.entity;

public class CharacterPersonality {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "character_id", nullable = false)
    private Character character;
    
    // Big Five Personality Traits (0-100)
    @Column(n = "openness")
    private Integer openness = 50;
    
    @Column(n = "conscientiousness")
    private Integer conscientiousness = 50;
    
    @Column(n = "extraversion")
    private Integer extraversion = 50;
    
    @Column(n = "agreeableness")
    private Integer agreeableness = 50;
    
    @Column(n = "neuroticism")
    private Integer neuroticism = 50;
    
    // Communication Style
    @Column(n = "formality_level")
    private Integer formalityLevel = 50;
    
    @Column(n = "humor_level")
    private Integer humorLevel = 50;
    
    @Column(n = "empathy_level")
    private Integer empathyLevel = 50;
    
    @Column(n = "assertiveness_level")
    private Integer assertivenessLevel = 50;
    
    // Behavioral Traits
    @ElementCollection
    @CollectionTable(n = "character_traits", 
        joinColumns = @JoinColumn(n = "personality_id"))
    @MapKeyColumn(n = "trait_n")
    @Column(n = "trait_value")
    private Map<String, String> customTraits = new HashMap<>();
    
    // Interests and Hobbies
    @ElementCollection
    @CollectionTable(n = "character_interests", 
        joinColumns = @JoinColumn(n = "personality_id"))
    @Column(n = "interest")
    private Set<String> interests = new HashSet<>();
    
    // Response Patterns
    @Column(n = "response_style", columnDefinition = "TEXT")
    private String responseStyle;
    
    @Column(n = "vocabulary_level")
    private String vocabularyLevel = "MEDIUM";
    
    @Column(n = "sentence_complexity")
    private String sentenceComplexity = "MEDIUM";
}
