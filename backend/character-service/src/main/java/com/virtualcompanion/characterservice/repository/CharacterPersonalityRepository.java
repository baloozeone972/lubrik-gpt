package com.virtualcompanion.characterservice.repository;

public interface CharacterPersonalityRepository extends JpaRepository<CharacterPersonality, UUID> {

    Optional<CharacterPersonality> findByCharacterId(UUID characterId);

    @Query("SELECT cp FROM CharacterPersonality cp WHERE cp.dominantTrait = :trait")
    List<CharacterPersonality> findByDominantTrait(@Param("trait") String trait);

    @Query("SELECT cp FROM CharacterPersonality cp WHERE " +
            "cp.openness >= :minOpenness AND cp.openness <= :maxOpenness AND " +
            "cp.conscientiousness >= :minConscientiousness AND cp.conscientiousness <= :maxConscientiousness")
    List<CharacterPersonality> findByTraitRanges(
            @Param("minOpenness") Double minOpenness,
            @Param("maxOpenness") Double maxOpenness,
            @Param("minConscientiousness") Double minConscientiousness,
            @Param("maxConscientiousness") Double maxConscientiousness
    );
}
