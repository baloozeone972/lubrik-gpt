// CreateCharacterRequest.java
package com.virtualcompanion.characterservice.dto;

import com.virtualcompanion.characterservice.entity.Character.Gender;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCharacterRequest {
    
    @NotBlank(message = "Character name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @NotNull(message = "Category is required")
    private String category;
    
    @NotNull(message = "Gender is required")
    private Gender gender;
    
    @Min(value = 18, message = "Character must be at least 18 years old")
    @Max(value = 100, message = "Character age cannot exceed 100")
    private Integer age;
    
    @Size(max = 1000, message = "Backstory cannot exceed 1000 characters")
    private String backstory;
    
    private List<String> tags;
    
    private PersonalityTraitsDto personalityTraits;
    
    private AppearanceDto appearance;
    
    private VoiceConfigDto voiceConfig;
    
    private List<DialogueExampleDto> dialogueExamples;
    
    private Boolean isPublic;
    
    private Boolean isNsfw;
}

// UpdateCharacterRequest.java
package com.virtualcompanion.characterservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCharacterRequest {
    
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    private String category;
    
    @Min(value = 18, message = "Character must be at least 18 years old")
    @Max(value = 100, message = "Character age cannot exceed 100")
    private Integer age;
    
    @Size(max = 1000, message = "Backstory cannot exceed 1000 characters")
    private String backstory;
    
    private List<String> tags;
    
    private PersonalityTraitsDto personalityTraits;
    
    private AppearanceDto appearance;
    
    private VoiceConfigDto voiceConfig;
    
    private List<DialogueExampleDto> dialogueExamples;
    
    private Boolean isPublic;
    
    private Boolean isNsfw;
}

// CharacterResponse.java
package com.virtualcompanion.characterservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CharacterResponse {
    
    private UUID id;
    private String name;
    private String description;
    private String category;
    private String gender;
    private Integer age;
    private String backstory;
    private List<String> tags;
    private String avatarUrl;
    private PersonalityTraitsDto personalityTraits;
    private AppearanceDto appearance;
    private VoiceConfigDto voiceConfig;
    private List<DialogueExampleDto> dialogueExamples;
    private Boolean isPublic;
    private Boolean isNsfw;
    private Boolean isActive;
    private UUID creatorId;
    private String creatorUsername;
    private Long totalConversations;
    private Double averageRating;
    private Long totalRatings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// PersonalityTraitsDto.java
package com.virtualcompanion.characterservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityTraitsDto {
    
    @NotNull
    @DecimalMin(value = "0.0", message = "Openness must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Openness cannot exceed 1.0")
    private Double openness;
    
    @NotNull
    @DecimalMin(value = "0.0", message = "Conscientiousness must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Conscientiousness cannot exceed 1.0")
    private Double conscientiousness;
    
    @NotNull
    @DecimalMin(value = "0.0", message = "Extraversion must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Extraversion cannot exceed 1.0")
    private Double extraversion;
    
    @NotNull
    @DecimalMin(value = "0.0", message = "Agreeableness must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Agreeableness cannot exceed 1.0")
    private Double agreeableness;
    
    @NotNull
    @DecimalMin(value = "0.0", message = "Neuroticism must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Neuroticism cannot exceed 1.0")
    private Double neuroticism;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String dominantTrait;
    
    @Size(max = 1000, message = "Behavior notes cannot exceed 1000 characters")
    private String behaviorNotes;
}

// AppearanceDto.java
package com.virtualcompanion.characterservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppearanceDto {
    
    @Size(max = 20, message = "Height cannot exceed 20 characters")
    private String height;
    
    @Size(max = 20, message = "Weight cannot exceed 20 characters")
    private String weight;
    
    @Size(max = 50, message = "Hair color cannot exceed 50 characters")
    private String hairColor;
    
    @Size(max = 50, message = "Hair style cannot exceed 50 characters")
    private String hairStyle;
    
    @Size(max = 50, message = "Eye color cannot exceed 50 characters")
    private String eyeColor;
    
    @Size(max = 50, message = "Skin tone cannot exceed 50 characters")
    private String skinTone;
    
    @Size(max = 100, message = "Body type cannot exceed 100 characters")
    private String bodyType;
    
    @Size(max = 100, message = "Ethnicity cannot exceed 100 characters")
    private String ethnicity;
    
    @Size(max = 500, message = "Clothing style cannot exceed 500 characters")
    private String clothingStyle;
    
    @Size(max = 500, message = "Distinguishing features cannot exceed 500 characters")
    private String distinguishingFeatures;
}

// VoiceConfigDto.java
package com.virtualcompanion.characterservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceConfigDto {
    
    @NotBlank(message = "Voice provider is required")
    private String provider;
    
    @NotBlank(message = "Voice ID is required")
    private String voiceId;
    
    @Size(max = 100, message = "Voice name cannot exceed 100 characters")
    private String voiceName;
    
    @Size(max = 50, message = "Language cannot exceed 50 characters")
    private String language;
    
    @DecimalMin(value = "0.5", message = "Pitch must be at least 0.5")
    @DecimalMax(value = "2.0", message = "Pitch cannot exceed 2.0")
    private Double pitch;
    
    @DecimalMin(value = "0.5", message = "Speed must be at least 0.5")
    @DecimalMax(value = "2.0", message = "Speed cannot exceed 2.0")
    private Double speed;
    
    @Size(max = 50, message = "Emotion cannot exceed 50 characters")
    private String emotion;
    
    @Size(max = 200, message = "Sample URL cannot exceed 200 characters")
    private String sampleUrl;
}

// DialogueExampleDto.java
package com.virtualcompanion.characterservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DialogueExampleDto {
    
    @NotBlank(message = "Context is required")
    @Size(max = 200, message = "Context cannot exceed 200 characters")
    private String context;
    
    @NotBlank(message = "User input is required")
    @Size(max = 500, message = "User input cannot exceed 500 characters")
    private String userInput;
    
    @NotBlank(message = "Character response is required")
    @Size(max = 1000, message = "Character response cannot exceed 1000 characters")
    private String characterResponse;
    
    @Size(max = 50, message = "Mood cannot exceed 50 characters")
    private String mood;
}

// CharacterSearchRequest.java
package com.virtualcompanion.characterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSearchRequest {
    
    private String query;
    private List<String> categories;
    private List<String> tags;
    private String gender;
    private Integer minAge;
    private Integer maxAge;
    private Double minRating;
    private Boolean isPublic;
    private Boolean isNsfw;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;
}

// CharacterSearchResponse.java
package com.virtualcompanion.characterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSearchResponse {
    
    private List<CharacterResponse> characters;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private Map<String, Map<String, Long>> facets;
}

// RateCharacterRequest.java
package com.virtualcompanion.characterservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateCharacterRequest {
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;
    
    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
}

// CharacterRatingResponse.java
package com.virtualcompanion.characterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterRatingResponse {
    
    private UUID id;
    private UUID characterId;
    private UUID userId;
    private String username;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}

// CharacterStatisticsResponse.java
package com.virtualcompanion.characterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterStatisticsResponse {
    
    private Long totalConversations;
    private Long uniqueUsers;
    private Double averageRating;
    private Long totalRatings;
    private Map<Integer, Long> ratingDistribution;
    private Long totalMessages;
    private Double averageConversationLength;
    private Map<String, Long> usageByDay;
    private Map<String, Long> usageByHour;
    private Double popularityScore;
}

// GenerateCharacterRequest.java
package com.virtualcompanion.characterservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateCharacterRequest {
    
    @NotBlank(message = "Prompt is required")
    @Size(max = 1000, message = "Prompt cannot exceed 1000 characters")
    private String prompt;
    
    private String category;
    
    private String gender;
    
    @Size(max = 100, message = "Style cannot exceed 100 characters")
    private String style;
    
    private PersonalityTraitsDto preferredTraits;
}

// CharacterImageUploadResponse.java
package com.virtualcompanion.characterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterImageUploadResponse {
    
    private UUID imageId;
    private String url;
    private String thumbnailUrl;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
}