package com.virtualcompanion.characterservice.service;

public interface CharacterSearchService {
    void indexCharacter(Character character);
    void updateCharacter(Character character);
    void deleteCharacter(UUID characterId);
    CharacterSearchResponse searchCharacters(CharacterSearchRequest request);
}
