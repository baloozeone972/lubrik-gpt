// CharacterRepository.java
package com.virtualcompanion.characterservice.repository;

import com.virtualcompanion.characterservice.entity.Character;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
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

// CharacterPersonalityRepository.java
package com.virtualcompanion.characterservice.repository;

import com.virtualcompanion.characterservice.entity.CharacterPersonality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
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

// CharacterAppearanceRepository.java
package com.virtualcompanion.characterservice.repository;

import com.virtualcompanion.characterservice.entity.CharacterAppearance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CharacterAppearanceRepository extends JpaRepository<CharacterAppearance, UUID> {
    
    Optional<CharacterAppearance> findByCharacterId(UUID characterId);
}

// CharacterVoiceRepository.java
package com.virtualcompanion.characterservice.repository;

import com.virtualcompanion.characterservice.entity.CharacterVoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CharacterVoiceRepository extends JpaRepository<CharacterVoice, UUID> {
    
    Optional<CharacterVoice> findByCharacterId(UUID characterId);
    
    @Query("SELECT cv FROM CharacterVoice cv WHERE cv.provider = :provider")
    List<CharacterVoice> findByProvider(@Param("provider") String provider);
    
    @Query("SELECT cv FROM CharacterVoice cv WHERE cv.language = :language")
    List<CharacterVoice> findByLanguage(@Param("language") String language);
}

// CharacterImageRepository.java
package com.virtualcompanion.characterservice.repository;

import com.virtualcompanion.characterservice.entity.CharacterImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
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

// CharacterTagRepository.java
package com.virtualcompanion.characterservice.repository;

import com.virtualcompanion.characterservice.entity.CharacterTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
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

// CharacterDialogueRepository.java
package com.virtualcompanion.characterservice.repository;

import com.virtualcompanion.characterservice.entity.CharacterDialogue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CharacterDialogueRepository extends JpaRepository<CharacterDialogue, UUID> {
    
    List<CharacterDialogue> findByCharacterIdOrderByCreatedAt(UUID characterId);
    
    @Query("SELECT cd FROM CharacterDialogue cd WHERE cd.characterId = :characterId AND cd.context = :context")
    List<CharacterDialogue> findByCharacterIdAndContext(@Param("characterId") UUID characterId, @Param("context") String context);
    
    @Query("SELECT cd FROM CharacterDialogue cd WHERE cd.characterId = :characterId AND cd.mood = :mood")
    List<CharacterDialogue> findByCharacterIdAndMood(@Param("characterId") UUID characterId, @Param("mood") String mood);
    
    void deleteByCharacterId(UUID characterId);
}

// UserCharacterRepository.java
package com.virtualcompanion.characterservice.repository;

import com.virtualcompanion.characterservice.entity.UserCharacter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
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

// CharacterRatingRepository.java
package com.virtualcompanion.characterservice.repository;

import com.virtualcompanion.characterservice.entity.CharacterRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
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