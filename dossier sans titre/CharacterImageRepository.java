package com.virtualcompanion.characterservice.repository;

public interface CharacterImageRepository extends JpaRepository<CharacterImage, UUID> {
    
    List<CharacterImage> findByCharacterIdOrderByUploadedAtDesc(UUID characterId);
    
    Optional<CharacterImage> findByCharacterIdAndIsPrimaryTrue(UUID characterId);
    
    @Query("SELECT ci FROM CharacterImage ci WHERE ci.characterId = :characterId AND ci.imageType = :type")
    List<CharacterImage> findByCharacterIdAndType(@Param("characterId") UUID characterId, @Param("type") String type);
    
    @Query("SELECT SUM(ci.fileSize) FROM CharacterImage ci WHERE ci.characterId = :characterId")
    Long getTotalImageSizeByCharacter(@Param("characterId") UUID characterId);
    
    @Query("SELECT COUNT(ci) FROM CharacterImage ci WHERE ci.characterId = :characterId")
    long countImagesByCharacter(@Param("characterId") UUID characterId);
}
