package com.virtualcompanion.characterservice.service;

public interface PersonalityEngineService {
    Character generateCharacter(GenerateCharacterRequest request);

    CharacterPersonality generatePersonality(GenerateCharacterRequest request);

    List<Character> getRecommendations(UUID userId, List<UserCharacter> userHistory, int limit);

    double calculatePopularityScore(long conversations, Double rating, Long ratingCount, long uniqueUsers);
}
