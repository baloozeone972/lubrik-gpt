package com.virtualcompanion.characterservice.repository;

public interface CharacterAppearanceRepository extends JpaRepository<CharacterAppearance, UUID> {
    
    Optional<CharacterAppearance> findByCharacterId(UUID characterId);
}
