package com.virtualcompanion.characterservice.entity;

public class CharacterAppearance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "character_id", nullable = false)
    private Character character;
    
    // Physical Attributes
    @Column(n = "age_appearance")
    private Integer ageAppearance;
    
    @Column(n = "gender")
    private String gender;
    
    @Column(n = "ethnicity")
    private String ethnicity;
    
    @Column(n = "height_cm")
    private Integer heightCm;
    
    @Column(n = "body_type")
    private String bodyType;
    
    @Column(n = "hair_color")
    private String hairColor;
    
    @Column(n = "hair_style")
    private String hairStyle;
    
    @Column(n = "eye_color")
    private String eyeColor;
    
    @Column(n = "skin_tone")
    private String skinTone;
    
    // Style and Clothing
    @Column(n = "clothing_style")
    private String clothingStyle;
    
    @Column(n = "distinctive_features", columnDefinition = "TEXT")
    private String distinctiveFeatures;
    
    // 3D Model References
    @Column(n = "model_file_url")
    private String modelFileUrl;
    
    @Column(n = "texture_file_url")
    private String textureFileUrl;
    
    @Column(n = "animation_set_id")
    private String animationSetId;
    
    // Avatar Generation Parameters
    @Column(n = "avatar_preset_id")
    private String avatarPresetId;
    
    @Column(n = "avatar_config", columnDefinition = "JSON")
    private String avatarConfig;
}
