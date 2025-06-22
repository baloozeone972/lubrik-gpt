package com.virtualcompanion.characterservice.repository;

public interface CharacterVoiceRepository extends JpaRepository<CharacterVoice, UUID> {

    Optional<CharacterVoice> findByCharacterId(UUID characterId);

    @Query("SELECT cv FROM CharacterVoice cv WHERE cv.provider = :provider")
    List<CharacterVoice> findByProvider(@Param("provider") String provider);

    @Query("SELECT cv FROM CharacterVoice cv WHERE cv.language = :language")
    List<CharacterVoice> findByLanguage(@Param("language") String language);
}
