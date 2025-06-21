package com.virtualcompanion.conversationservice.repository;

public interface CharacterContextRepository extends MongoRepository<CharacterContext, String> {
    
    Optional<CharacterContext> findByUserIdAndCharacterId(UUID userId, UUID characterId);
    
    @Query("{ 'userId': ?0, 'characterId': ?1, 'isActive': true }")
    Optional<CharacterContext> findActiveCharacterContext(UUID userId, UUID characterId);
    
    void deleteByUserIdAndCharacterId(UUID userId, UUID characterId);
}
