// CharacterService.java
package com.virtualcompanion.characterservice.service;

import com.virtualcompanion.characterservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CharacterService {
    
    CharacterResponse createCharacter(UUID userId, CreateCharacterRequest request);
    
    CharacterResponse getCharacterById(UUID characterId);
    
    CharacterResponse updateCharacter(UUID userId, UUID characterId, UpdateCharacterRequest request);
    
    void deleteCharacter(UUID userId, UUID characterId);
    
    Page<CharacterResponse> getUserCharacters(UUID userId, Pageable pageable);
    
    Page<CharacterResponse> getPublicCharacters(Pageable pageable);
    
    CharacterSearchResponse searchCharacters(CharacterSearchRequest request);
    
    CharacterResponse rateCharacter(UUID userId, UUID characterId, RateCharacterRequest request);
    
    Page<CharacterRatingResponse> getCharacterRatings(UUID characterId, Pageable pageable);
    
    CharacterStatisticsResponse getCharacterStatistics(UUID characterId);
    
    CharacterResponse toggleFavorite(UUID userId, UUID characterId);
    
    List<CharacterResponse> getUserFavorites(UUID userId);
    
    CharacterResponse generateCharacter(UUID userId, GenerateCharacterRequest request);
    
    CharacterImageUploadResponse uploadCharacterImage(UUID userId, UUID characterId, MultipartFile file);
    
    void deleteCharacterImage(UUID userId, UUID characterId, UUID imageId);
    
    List<CharacterImageUploadResponse> getCharacterImages(UUID characterId);
    
    void recordInteraction(UUID userId, UUID characterId);
    
    List<CharacterResponse> getRecommendedCharacters(UUID userId, int limit);
    
    List<CharacterResponse> getTrendingCharacters(int limit);
    
    void updateCharacterPopularity(UUID characterId);
}

// CharacterServiceImpl.java
package com.virtualcompanion.characterservice.service.impl;

import com.virtualcompanion.characterservice.dto.*;
import com.virtualcompanion.characterservice.entity.*;
import com.virtualcompanion.characterservice.entity.Character;
import com.virtualcompanion.characterservice.exception.*;
import com.virtualcompanion.characterservice.mapper.CharacterMapper;
import com.virtualcompanion.characterservice.repository.*;
import com.virtualcompanion.characterservice.service.CharacterSearchService;
import com.virtualcompanion.characterservice.service.CharacterService;
import com.virtualcompanion.characterservice.service.ImageStorageService;
import com.virtualcompanion.characterservice.service.PersonalityEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CharacterServiceImpl implements CharacterService {
    
    private final CharacterRepository characterRepository;
    private final CharacterPersonalityRepository personalityRepository;
    private final CharacterAppearanceRepository appearanceRepository;
    private final CharacterVoiceRepository voiceRepository;
    private final CharacterImageRepository imageRepository;
    private final CharacterTagRepository tagRepository;
    private final CharacterDialogueRepository dialogueRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterRatingRepository ratingRepository;
    private final CharacterMapper characterMapper;
    private final CharacterSearchService searchService;
    private final ImageStorageService imageStorageService;
    private final PersonalityEngineService personalityEngine;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${character.defaults.max-characters-per-user}")
    private int maxCharactersPerUser;
    
    @Value("${character.defaults.max-free-characters}")
    private int maxFreeCharacters;
    
    @Override
    public CharacterResponse createCharacter(UUID userId, CreateCharacterRequest request) {
        log.info("Creating character for user: {}", userId);
        
        // Check user's character limit
        long userCharacterCount = characterRepository.countActiveCharactersByCreator(userId);
        if (userCharacterCount >= maxCharactersPerUser) {
            throw new CharacterLimitExceededException("Character limit exceeded. Maximum allowed: " + maxCharactersPerUser);
        }
        
        // Create character entity
        Character character = Character.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .gender(request.getGender())
                .age(request.getAge())
                .backstory(request.getBackstory())
                .creatorId(userId)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .isNsfw(request.getIsNsfw() != null ? request.getIsNsfw() : false)
                .isActive(true)
                .totalConversations(0L)
                .popularityScore(0.0)
                .build();
        
        character = characterRepository.save(character);
        
        // Save personality traits
        if (request.getPersonalityTraits() != null) {
            CharacterPersonality personality = characterMapper.toPersonalityEntity(request.getPersonalityTraits());
            personality.setCharacterId(character.getId());
            personalityRepository.save(personality);
        }
        
        // Save appearance
        if (request.getAppearance() != null) {
            CharacterAppearance appearance = characterMapper.toAppearanceEntity(request.getAppearance());
            appearance.setCharacterId(character.getId());
            appearanceRepository.save(appearance);
        }
        
        // Save voice config
        if (request.getVoiceConfig() != null) {
            CharacterVoice voice = characterMapper.toVoiceEntity(request.getVoiceConfig());
            voice.setCharacterId(character.getId());
            voiceRepository.save(voice);
        }
        
        // Save tags
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<CharacterTag> tags = new HashSet<>();
            for (String tagName : request.getTags()) {
                CharacterTag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(CharacterTag.builder().name(tagName).build()));
                tags.add(tag);
            }
            character.setTags(tags);
            characterRepository.save(character);
        }
        
        // Save dialogue examples
        if (request.getDialogueExamples() != null) {
            for (DialogueExampleDto example : request.getDialogueExamples()) {
                CharacterDialogue dialogue = characterMapper.toDialogueEntity(example);
                dialogue.setCharacterId(character.getId());
                dialogueRepository.save(dialogue);
            }
        }
        
        // Publish event
        kafkaTemplate.send("character-events", "character.created", 
            Map.of("characterId", character.getId(), "userId", userId));
        
        // Index in Elasticsearch
        searchService.indexCharacter(character);
        
        log.info("Character created successfully: {}", character.getId());
        return buildCharacterResponse(character);
    }
    
    @Override
    @Cacheable(value = "characters", key = "#characterId")
    public CharacterResponse getCharacterById(UUID characterId) {
        Character character = characterRepository.findByIdAndIsActiveTrue(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found: " + characterId));
        
        return buildCharacterResponse(character);
    }
    
    @Override
    @CacheEvict(value = "characters", key = "#characterId")
    public CharacterResponse updateCharacter(UUID userId, UUID characterId, UpdateCharacterRequest request) {
        Character character = characterRepository.findByIdAndIsActiveTrue(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found: " + characterId));
        
        // Check ownership
        if (!character.getCreatorId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to update this character");
        }
        
        // Update basic info
        if (request.getName() != null) {
            character.setName(request.getName());
        }
        if (request.getDescription() != null) {
            character.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            character.setCategory(request.getCategory());
        }
        if (request.getAge() != null) {
            character.setAge(request.getAge());
        }
        if (request.getBackstory() != null) {
            character.setBackstory(request.getBackstory());
        }
        if (request.getIsPublic() != null) {
            character.setIsPublic(request.getIsPublic());
        }
        if (request.getIsNsfw() != null) {
            character.setIsNsfw(request.getIsNsfw());
        }
        
        character.setUpdatedAt(LocalDateTime.now());
        character = characterRepository.save(character);
        
        // Update personality if provided
        if (request.getPersonalityTraits() != null) {
            CharacterPersonality personality = personalityRepository.findByCharacterId(characterId)
                    .orElse(new CharacterPersonality());
            characterMapper.updatePersonalityEntity(personality, request.getPersonalityTraits());
            personality.setCharacterId(characterId);
            personalityRepository.save(personality);
        }
        
        // Update appearance if provided
        if (request.getAppearance() != null) {
            CharacterAppearance appearance = appearanceRepository.findByCharacterId(characterId)
                    .orElse(new CharacterAppearance());
            characterMapper.updateAppearanceEntity(appearance, request.getAppearance());
            appearance.setCharacterId(characterId);
            appearanceRepository.save(appearance);
        }
        
        // Update voice if provided
        if (request.getVoiceConfig() != null) {
            CharacterVoice voice = voiceRepository.findByCharacterId(characterId)
                    .orElse(new CharacterVoice());
            characterMapper.updateVoiceEntity(voice, request.getVoiceConfig());
            voice.setCharacterId(characterId);
            voiceRepository.save(voice);
        }
        
        // Update tags if provided
        if (request.getTags() != null) {
            Set<CharacterTag> tags = new HashSet<>();
            for (String tagName : request.getTags()) {
                CharacterTag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(CharacterTag.builder().name(tagName).build()));
                tags.add(tag);
            }
            character.setTags(tags);
            characterRepository.save(character);
        }
        
        // Update dialogue examples if provided
        if (request.getDialogueExamples() != null) {
            // Remove existing dialogues
            dialogueRepository.deleteByCharacterId(characterId);
            
            // Add new dialogues
            for (DialogueExampleDto example : request.getDialogueExamples()) {
                CharacterDialogue dialogue = characterMapper.toDialogueEntity(example);
                dialogue.setCharacterId(characterId);
                dialogueRepository.save(dialogue);
            }
        }
        
        // Update search index
        searchService.updateCharacter(character);
        
        // Publish event
        kafkaTemplate.send("character-events", "character.updated", 
            Map.of("characterId", characterId, "userId", userId));
        
        return buildCharacterResponse(character);
    }
    
    @Override
    @CacheEvict(value = "characters", key = "#characterId")
    public void deleteCharacter(UUID userId, UUID characterId) {
        Character character = characterRepository.findByIdAndIsActiveTrue(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found: " + characterId));
        
        // Check ownership
        if (!character.getCreatorId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to delete this character");
        }
        
        // Soft delete
        character.setIsActive(false);
        character.setDeletedAt(LocalDateTime.now());
        characterRepository.save(character);
        
        // Remove from search index
        searchService.deleteCharacter(characterId);
        
        // Publish event
        kafkaTemplate.send("character-events", "character.deleted", 
            Map.of("characterId", characterId, "userId", userId));
        
        log.info("Character deleted: {}", characterId);
    }
    
    @Override
    public Page<CharacterResponse> getUserCharacters(UUID userId, Pageable pageable) {
        return characterRepository.findByCreatorIdAndIsActiveTrue(userId, pageable)
                .map(this::buildCharacterResponse);
    }
    
    @Override
    public Page<CharacterResponse> getPublicCharacters(Pageable pageable) {
        return characterRepository.findByIsPublicTrueAndIsActiveTrue(pageable)
                .map(this::buildCharacterResponse);
    }
    
    @Override
    public CharacterSearchResponse searchCharacters(CharacterSearchRequest request) {
        return searchService.searchCharacters(request);
    }
    
    @Override
    public CharacterResponse rateCharacter(UUID userId, UUID characterId, RateCharacterRequest request) {
        Character character = characterRepository.findByIdAndIsActiveTrue(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found: " + characterId));
        
        // Check if user has already rated
        Optional<CharacterRating> existingRating = ratingRepository.findByCharacterIdAndUserId(characterId, userId);
        
        CharacterRating rating;
        if (existingRating.isPresent()) {
            // Update existing rating
            rating = existingRating.get();
            rating.setRating(request.getRating());
            rating.setComment(request.getComment());
            rating.setUpdatedAt(LocalDateTime.now());
        } else {
            // Create new rating
            rating = CharacterRating.builder()
                    .characterId(characterId)
                    .userId(userId)
                    .rating(request.getRating())
                    .comment(request.getComment())
                    .build();
        }
        
        ratingRepository.save(rating);
        
        // Update character average rating
        Double avgRating = ratingRepository.getAverageRatingByCharacter(characterId);
        Long totalRatings = ratingRepository.getTotalRatingsByCharacter(characterId);
        
        character.setAverageRating(avgRating);
        character.setTotalRatings(totalRatings);
        characterRepository.save(character);
        
        // Update popularity score
        updateCharacterPopularity(characterId);
        
        // Publish event
        kafkaTemplate.send("character-events", "character.rated", 
            Map.of("characterId", characterId, "userId", userId, "rating", request.getRating()));
        
        return buildCharacterResponse(character);
    }
    
    @Override
    public Page<CharacterRatingResponse> getCharacterRatings(UUID characterId, Pageable pageable) {
        return ratingRepository.findByCharacterIdOrderByCreatedAtDesc(characterId, pageable)
                .map(characterMapper::toRatingResponse);
    }
    
    @Override
    public CharacterStatisticsResponse getCharacterStatistics(UUID characterId) {
        Character character = characterRepository.findByIdAndIsActiveTrue(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found: " + characterId));
        
        // Get rating distribution
        List<Object[]> ratingDist = ratingRepository.getRatingDistributionByCharacter(characterId);
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (Object[] row : ratingDist) {
            ratingDistribution.put((Integer) row[0], (Long) row[1]);
        }
        
        // Get unique users
        long uniqueUsers = userCharacterRepository.countUniqueUsersByCharacter(characterId);
        
        return CharacterStatisticsResponse.builder()
                .totalConversations(character.getTotalConversations())
                .uniqueUsers(uniqueUsers)
                .averageRating(character.getAverageRating())
                .totalRatings(character.getTotalRatings())
                .ratingDistribution(ratingDistribution)
                .popularityScore(character.getPopularityScore())
                .build();
    }
    
    @Override
    public CharacterResponse toggleFavorite(UUID userId, UUID characterId) {
        Character character = characterRepository.findByIdAndIsActiveTrue(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found: " + characterId));
        
        UserCharacter userCharacter = userCharacterRepository.findByUserIdAndCharacterId(userId, characterId)
                .orElseGet(() -> UserCharacter.builder()
                        .userId(userId)
                        .characterId(characterId)
                        .isActive(true)
                        .interactionCount(0L)
                        .build());
        
        userCharacter.setIsFavorite(!userCharacter.getIsFavorite());
        userCharacterRepository.save(userCharacter);
        
        // Publish event
        kafkaTemplate.send("character-events", "character.favorite.toggled", 
            Map.of("characterId", characterId, "userId", userId, "isFavorite", userCharacter.getIsFavorite()));
        
        return buildCharacterResponse(character);
    }
    
    @Override
    public List<CharacterResponse> getUserFavorites(UUID userId) {
        List<UserCharacter> favorites = userCharacterRepository.findByUserIdAndIsFavoriteTrue(userId);
        
        return favorites.stream()
                .map(uc -> characterRepository.findByIdAndIsActiveTrue(uc.getCharacterId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::buildCharacterResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public CharacterResponse generateCharacter(UUID userId, GenerateCharacterRequest request) {
        log.info("Generating character for user: {} with prompt: {}", userId, request.getPrompt());
        
        // Use AI to generate character based on prompt
        Character generatedCharacter = personalityEngine.generateCharacter(request);
        generatedCharacter.setCreatorId(userId);
        generatedCharacter.setIsActive(true);
        generatedCharacter.setTotalConversations(0L);
        generatedCharacter.setPopularityScore(0.0);
        
        // Save character
        generatedCharacter = characterRepository.save(generatedCharacter);
        
        // Generate and save personality
        CharacterPersonality personality = personalityEngine.generatePersonality(request);
        personality.setCharacterId(generatedCharacter.getId());
        personalityRepository.save(personality);
        
        // Index in search
        searchService.indexCharacter(generatedCharacter);
        
        // Publish event
        kafkaTemplate.send("character-events", "character.generated", 
            Map.of("characterId", generatedCharacter.getId(), "userId", userId));
        
        return buildCharacterResponse(generatedCharacter);
    }
    
    @Override
    public CharacterImageUploadResponse uploadCharacterImage(UUID userId, UUID characterId, MultipartFile file) {
        Character character = characterRepository.findByIdAndIsActiveTrue(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found: " + characterId));
        
        // Check ownership
        if (!character.getCreatorId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to upload images for this character");
        }
        
        // Upload image
        CharacterImage image = imageStorageService.uploadImage(characterId, file);
        
        // Set as primary if it's the first image
        long imageCount = imageRepository.countImagesByCharacter(characterId);
        if (imageCount == 0) {
            image.setIsPrimary(true);
            character.setAvatarUrl(image.getImageUrl());
            characterRepository.save(character);
        }
        
        image = imageRepository.save(image);
        
        // Update search index with new image
        searchService.updateCharacter(character);
        
        return characterMapper.toImageUploadResponse(image);
    }
    
    @Override
    public void deleteCharacterImage(UUID userId, UUID characterId, UUID imageId) {
        Character character = characterRepository.findByIdAndIsActiveTrue(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found: " + characterId));
        
        // Check ownership
        if (!character.getCreatorId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to delete images for this character");
        }
        
        CharacterImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));
        
        // Delete from storage
        imageStorageService.deleteImage(image);
        
        // Delete from database
        imageRepository.delete(image);
        
        // If it was primary, set another image as primary
        if (image.getIsPrimary()) {
            List<CharacterImage> remainingImages = imageRepository.findByCharacterIdOrderByUploadedAtDesc(characterId);
            if (!remainingImages.isEmpty()) {
                CharacterImage newPrimary = remainingImages.get(0);
                newPrimary.setIsPrimary(true);
                imageRepository.save(newPrimary);
                character.setAvatarUrl(newPrimary.getImageUrl());
            } else {
                character.setAvatarUrl(null);
            }
            characterRepository.save(character);
        }
    }
    
    @Override
    public List<CharacterImageUploadResponse> getCharacterImages(UUID characterId) {
        return imageRepository.findByCharacterIdOrderByUploadedAtDesc(characterId).stream()
                .map(characterMapper::toImageUploadResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void recordInteraction(UUID userId, UUID characterId) {
        UserCharacter userCharacter = userCharacterRepository.findByUserIdAndCharacterId(userId, characterId)
                .orElseGet(() -> UserCharacter.builder()
                        .userId(userId)
                        .characterId(characterId)
                        .isActive(true)
                        .isFavorite(false)
                        .interactionCount(0L)
                        .build());
        
        userCharacterRepository.updateInteraction(userCharacter.getId(), LocalDateTime.now());
        characterRepository.incrementConversationCount(characterId);
        
        // Update popularity asynchronously
        kafkaTemplate.send("character-events", "character.interaction", 
            Map.of("characterId", characterId, "userId", userId));
    }
    
    @Override
    public List<CharacterResponse> getRecommendedCharacters(UUID userId, int limit) {
        // Get user's interaction history
        List<UserCharacter> userHistory = userCharacterRepository
                .findActiveUserCharacters(userId, PageRequest.of(0, 10));
        
        // Get characters similar to user's favorites
        List<Character> recommendations = personalityEngine.getRecommendations(userId, userHistory, limit);
        
        return recommendations.stream()
                .map(this::buildCharacterResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CharacterResponse> getTrendingCharacters(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> trending = userCharacterRepository
                .findMostUsedCharactersSince(since, PageRequest.of(0, limit));
        
        List<UUID> characterIds = trending.stream()
                .map(row -> (UUID) row[0])
                .collect(Collectors.toList());
        
        return characterRepository.findAllById(characterIds).stream()
                .filter(Character::getIsActive)
                .map(this::buildCharacterResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateCharacterPopularity(UUID characterId) {
        Character character = characterRepository.findById(characterId).orElse(null);
        if (character == null) return;
        
        // Calculate popularity score based on multiple factors
        double score = personalityEngine.calculatePopularityScore(
                character.getTotalConversations(),
                character.getAverageRating(),
                character.getTotalRatings(),
                userCharacterRepository.countUniqueUsersByCharacter(characterId)
        );
        
        characterRepository.updatePopularityScore(characterId, score);
    }
    
    private CharacterResponse buildCharacterResponse(Character character) {
        CharacterResponse response = characterMapper.toResponse(character);
        
        // Load related data
        personalityRepository.findByCharacterId(character.getId())
                .ifPresent(p -> response.setPersonalityTraits(characterMapper.toPersonalityDto(p)));
        
        appearanceRepository.findByCharacterId(character.getId())
                .ifPresent(a -> response.setAppearance(characterMapper.toAppearanceDto(a)));
        
        voiceRepository.findByCharacterId(character.getId())
                .ifPresent(v -> response.setVoiceConfig(characterMapper.toVoiceDto(v)));
        
        List<CharacterDialogue> dialogues = dialogueRepository.findByCharacterIdOrderByCreatedAt(character.getId());
        response.setDialogueExamples(dialogues.stream()
                .map(characterMapper::toDialogueDto)
                .collect(Collectors.toList()));
        
        return response;
    }
}