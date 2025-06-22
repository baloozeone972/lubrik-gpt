package com.virtualcompanion.characterservice.repository;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, UUID> {

    Optional<UserCharacter> findByUserIdAndCharacterId(UUID userId, UUID characterId);

    Page<UserCharacter> findByUserIdOrderByLastInteractionDesc(UUID userId, Pageable pageable);

    List<UserCharacter> findByUserIdAndIsFavoriteTrue(UUID userId);

    @Query("SELECT uc FROM UserCharacter uc WHERE uc.userId = :userId AND uc.isActive = true ORDER BY uc.lastInteraction DESC")
    List<UserCharacter> findActiveUserCharacters(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT uc.userId) FROM UserCharacter uc WHERE uc.characterId = :characterId")
    long countUniqueUsersByCharacter(@Param("characterId") UUID characterId);

    @Query("SELECT uc.characterId, COUNT(uc) as userCount FROM UserCharacter uc " +
            "WHERE uc.lastInteraction >= :since " +
            "GROUP BY uc.characterId ORDER BY userCount DESC")
    List<Object[]> findMostUsedCharactersSince(@Param("since") LocalDateTime since, Pageable pageable);

    @Modifying
    @Query("UPDATE UserCharacter uc SET uc.lastInteraction = :now, uc.interactionCount = uc.interactionCount + 1 WHERE uc.id = :id")
    void updateInteraction(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
