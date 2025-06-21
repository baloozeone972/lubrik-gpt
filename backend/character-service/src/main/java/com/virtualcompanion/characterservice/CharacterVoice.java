package com.virtualcompanion.characterservice;

public class CharacterVoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "character_id", nullable = false)
    private Character character;
    
    @Column(n = "language_code", nullable = false)
    private String languageCode;
    
    @Column(n = "voice_id")
    private String voiceId;
    
    @Column(n = "voice_provider")
    private String voiceProvider;
    
    // Voice Characteristics
    @Column(n = "pitch")
    private Float pitch = 0.0f;
    
    @Column(n = "speed")
    private Float speed = 1.0f;
    
    @Column(n = "tone")
    private String tone;
    
    @Column(n = "accent")
    private String accent;
    
    @Column(n = "emotion_range")
    private String emotionRange = "NORMAL";
    
    // Voice Sample
    @Column(n = "sample_audio_url")
    private String sampleAudioUrl;
    
    @Column(n = "is_default")
    private boolean isDefault = false;
    
    @Column(n = "is_premium")
    private boolean isPremium = false;
}
