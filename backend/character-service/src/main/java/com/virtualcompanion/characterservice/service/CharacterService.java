package com.virtualcompanion.characterservice.service;

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
