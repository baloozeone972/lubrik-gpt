// CharacterServiceApplication.java
package com.virtualcompanion.characterservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.virtualcompanion.characterservice.repository.jpa")
@EnableElasticsearchRepositories(basePackages = "com.virtualcompanion.characterservice.repository.elasticsearch")
public class CharacterServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CharacterServiceApplication.class, args);
    }
}

// ===== ENTITIES =====

// Character.java
package com.virtualcompanion.characterservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(n = "characters", indexes = {
    @Index(n = "idx_character_n", columnList = "n"),
    @Index(n = "idx_character_status", columnList = "status"),
    @Index(n = "idx_character_category", columnList = "category"),
    @Index(n = "idx_character_popularity", columnList = "popularity_score")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
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

// CharacterStatus.java
package com.virtualcompanion.characterservice.entity;

public enum CharacterStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    PUBLISHED,
    SUSPENDED,
    DELETED
}

// CharacterCategory.java
package com.virtualcompanion.characterservice.entity;

public enum CharacterCategory {
    FRIEND,
    ROMANTIC,
    MENTOR,
    COMPANION,
    FANTASY,
    HISTORICAL,
    CELEBRITY,
    CUSTOM
}

// AccessLevel.java
package com.virtualcompanion.characterservice.entity;

public enum AccessLevel {
    FREE,
    STANDARD,
    PREMIUM,
    VIP,
    EXCLUSIVE
}

// CharacterPersonality.java
package com.virtualcompanion.characterservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(n = "character_personalities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
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

// CharacterAppearance.java
package com.virtualcompanion.characterservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(n = "character_appearances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
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

// CharacterVoice.java
package com.virtualcompanion.characterservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(n = "character_voices", indexes = {
    @Index(n = "idx_voice_character", columnList = "character_id"),
    @Index(n = "idx_voice_language", columnList = "language_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
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

// CharacterImage.java
package com.virtualcompanion.characterservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "character_images", indexes = {
    @Index(n = "idx_image_character", columnList = "character_id"),
    @Index(n = "idx_image_type", columnList = "image_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CharacterImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "character_id", nullable = false)
    private Character character;
    
    @Column(n = "image_url", nullable = false)
    private String imageUrl;
    
    @Column(n = "thumbnail_url")
    private String thumbnailUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "image_type", nullable = false)
    private ImageType imageType;
    
    @Column(n = "is_primary")
    private boolean isPrimary = false;
    
    @Column(n = "width")
    private Integer width;
    
    @Column(n = "height")
    private Integer height;
    
    @Column(n = "file_size")
    private Long fileSize;
    
    @Column(n = "mime_type")
    private String mimeType;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum ImageType {
        PROFILE,
        FULL_BODY,
        EXPRESSION,
        OUTFIT,
        SCENE,
        PROMOTIONAL
    }
}

// CharacterTag.java
package com.virtualcompanion.characterservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(n = "character_tags", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"character_id", "tag_n"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
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

// CharacterDialogue.java
package com.virtualcompanion.characterservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(n = "character_dialogues")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
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

// UserCharacter.java
package com.virtualcompanion.characterservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "user_characters", indexes = {
    @Index(n = "idx_user_character", columnList = "user_id,character_id", unique = true),
    @Index(n = "idx_user_characters", columnList = "user_id"),
    @Index(n = "idx_character_users", columnList = "character_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
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

// CharacterRating.java
package com.virtualcompanion.characterservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "character_ratings", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "character_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CharacterRating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "character_id", nullable = false)
    private Character character;
    
    @Column(nullable = false)
    private Integer rating;
    
    @Column(columnDefinition = "TEXT")
    private String review;
    
    @Column(n = "is_verified_purchase")
    private boolean isVerifiedPurchase = false;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}