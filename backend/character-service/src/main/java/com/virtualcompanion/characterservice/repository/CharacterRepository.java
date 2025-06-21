package com.virtualcompanion.characterservice.repository;

public interface CharacterRepository extends JpaRepository<Character, UUID> {
    
    Optional<Character> findByIdAndIsActiveTrue(UUID id);
    
    Page<Character> findByCreatorIdAndIsActiveTrue(UUID creatorId, Pageable pageable);
    
    Page<Character> findByIsPublicTrueAndIsActiveTrue(Pageable pageable);
    
    @Query("SELECT c FROM Character c WHERE c.category = :category AND c.isPublic = true AND c.isActive = true")
    Page<Character> findByCategoryAndPublic(@Param("category") String category, Pageable pageable);
    
    @Query("SELECT c FROM Character c JOIN c.tags t WHERE t.name IN :tags AND c.isPublic = true AND c.isActive = true")
    Page<Character> findByTagsAndPublic(@Param("tags") List<String> tags, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM Character c WHERE c.creatorId = :creatorId AND c.isActive = true")
    long countActiveCharactersByCreator(@Param("creatorId") UUID creatorId);
    
    @Query("SELECT c FROM Character c WHERE c.isPublic = true AND c.isActive = true ORDER BY c.popularityScore DESC")
    List<Character> findTopPopularCharacters(Pageable pageable);
    
    @Query("SELECT c FROM Character c WHERE c.createdAt >= :date AND c.isPublic = true AND c.isActive = true")
    List<Character> findNewCharacters(@Param("date") LocalDateTime date, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Character c SET c.totalConversations = c.totalConversations + 1 WHERE c.id = :characterId")
    void incrementConversationCount(@Param("characterId") UUID characterId);
    
    @Modifying
    @Query("UPDATE Character c SET c.popularityScore = :score WHERE c.id = :characterId")
    void updatePopularityScore(@Param("characterId") UUID characterId, @Param("score") Double score);
    
    @Query(value = "SELECT * FROM characters WHERE to_tsvector('english', name || ' ' || COALESCE(description, '')) @@ plainto_tsquery('english', :query) " +
                   "AND is_public = true AND is_active = true", nativeQuery = true)
    Page<Character> searchCharacters(@Param("query") String query, Pageable pageable);
}
