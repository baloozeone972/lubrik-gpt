package com.virtualcompanion.characterservice.repository;

public interface CharacterRatingRepository extends JpaRepository<CharacterRating, UUID> {

    Optional<CharacterRating> findByCharacterIdAndUserId(UUID characterId, UUID userId);

    Page<CharacterRating> findByCharacterIdOrderByCreatedAtDesc(UUID characterId, Pageable pageable);

    @Query("SELECT AVG(cr.rating) FROM CharacterRating cr WHERE cr.characterId = :characterId")
    Double getAverageRatingByCharacter(@Param("characterId") UUID characterId);

    @Query("SELECT COUNT(cr) FROM CharacterRating cr WHERE cr.characterId = :characterId")
    Long getTotalRatingsByCharacter(@Param("characterId") UUID characterId);

    @Query("SELECT cr.rating, COUNT(cr) FROM CharacterRating cr WHERE cr.characterId = :characterId GROUP BY cr.rating")
    List<Object[]> getRatingDistributionByCharacter(@Param("characterId") UUID characterId);

    @Query("SELECT cr FROM CharacterRating cr WHERE cr.characterId = :characterId AND cr.comment IS NOT NULL ORDER BY cr.createdAt DESC")
    Page<CharacterRating> findReviewsByCharacter(@Param("characterId") UUID characterId, Pageable pageable);
}
