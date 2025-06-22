package com.virtualcompanion.characterservice.repository;

public interface CharacterDialogueRepository extends JpaRepository<CharacterDialogue, UUID> {

    List<CharacterDialogue> findByCharacterIdOrderByCreatedAt(UUID characterId);

    @Query("SELECT cd FROM CharacterDialogue cd WHERE cd.characterId = :characterId AND cd.context = :context")
    List<CharacterDialogue> findByCharacterIdAndContext(@Param("characterId") UUID characterId, @Param("context") String context);

    @Query("SELECT cd FROM CharacterDialogue cd WHERE cd.characterId = :characterId AND cd.mood = :mood")
    List<CharacterDialogue> findByCharacterIdAndMood(@Param("characterId") UUID characterId, @Param("mood") String mood);

    void deleteByCharacterId(UUID characterId);
}
