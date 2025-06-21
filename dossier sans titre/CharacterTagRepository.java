package com.virtualcompanion.characterservice.repository;

public interface CharacterTagRepository extends JpaRepository<CharacterTag, UUID> {
    
    Optional<CharacterTag> findByName(String name);
    
    List<CharacterTag> findByNameIn(List<String> names);
    
    @Query("SELECT t FROM CharacterTag t JOIN t.characters c WHERE c.id = :characterId")
    List<CharacterTag> findByCharacterId(@Param("characterId") UUID characterId);
    
    @Query("SELECT t.name, COUNT(c) as count FROM CharacterTag t JOIN t.characters c " +
           "WHERE c.isPublic = true AND c.isActive = true " +
           "GROUP BY t.name ORDER BY count DESC")
    List<Object[]> findPopularTags(Pageable pageable);
    
    @Query("SELECT DISTINCT t FROM CharacterTag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<CharacterTag> searchTags(@Param("query") String query);
}
