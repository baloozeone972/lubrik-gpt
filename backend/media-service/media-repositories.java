// MediaFileRepository.java
package com.virtualcompanion.mediaservice.repository;

import com.virtualcompanion.mediaservice.entity.MediaFile;
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
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    
    Optional<MediaFile> findByIdAndUserId(UUID id, UUID userId);
    
    Page<MediaFile> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);
    
    Page<MediaFile> findByCharacterIdAndDeletedAtIsNull(UUID characterId, Pageable pageable);
    
    Page<MediaFile> findByConversationIdAndDeletedAtIsNull(UUID conversationId, Pageable pageable);
    
    @Query("SELECT m FROM MediaFile m WHERE m.userId = :userId AND m.contentType LIKE :contentType% AND m.deletedAt IS NULL")
    Page<MediaFile> findByUserIdAndContentTypeStartingWith(@Param("userId") UUID userId, 
                                                           @Param("contentType") String contentType, 
                                                           Pageable pageable);
    
    @Query("SELECT SUM(m.fileSize) FROM MediaFile m WHERE m.userId = :userId AND m.deletedAt IS NULL")
    Long getTotalStorageSizeByUser(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(m) FROM MediaFile m WHERE m.userId = :userId AND m.contentType LIKE :contentType% AND m.deletedAt IS NULL")
    Long countByUserIdAndContentType(@Param("userId") UUID userId, @Param("contentType") String contentType);
    
    @Query("SELECT m FROM MediaFile m WHERE m.processingStatus = :status AND m.createdAt < :threshold")
    List<MediaFile> findStuckProcessingFiles(@Param("status") String status, @Param("threshold") LocalDateTime threshold);
    
    @Modifying
    @Query("UPDATE MediaFile m SET m.processingStatus = :status WHERE m.id = :id")
    void updateProcessingStatus(@Param("id") UUID id, @Param("status") String status);
    
    @Modifying
    @Query("UPDATE MediaFile m SET m.deletedAt = :now WHERE m.id = :id")
    void softDelete(@Param("id") UUID id, @Param("now") LocalDateTime now);
    
    List<MediaFile> findByDeletedAtIsNotNullAndDeletedAtBefore(LocalDateTime threshold);
}

// MediaVariantRepository.java
package com.virtualcompanion.mediaservice.repository;

import com.virtualcompanion.mediaservice.entity.MediaVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaVariantRepository extends JpaRepository<MediaVariant, UUID> {
    
    List<MediaVariant> findByMediaFileId(UUID mediaFileId);
    
    Optional<MediaVariant> findByMediaFileIdAndVariantTypeAndQuality(UUID mediaFileId, String variantType, String quality);
    
    @Query("SELECT SUM(v.fileSize) FROM MediaVariant v WHERE v.mediaFileId = :mediaFileId")
    Long getTotalVariantsSizeByMediaFile(@Param("mediaFileId") UUID mediaFileId);
    
    void deleteByMediaFileId(UUID mediaFileId);
}

// StreamingSessionRepository.java
package com.virtualcompanion.mediaservice.repository;

import com.virtualcompanion.mediaservice.entity.StreamingSession;
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
public interface StreamingSessionRepository extends JpaRepository<StreamingSession, UUID> {
    
    Optional<StreamingSession> findBySessionId(String sessionId);
    
    List<StreamingSession> findByUserIdAndStatus(UUID userId, String status);
    
    List<StreamingSession> findByCharacterIdAndStatus(UUID characterId, String status);
    
    @Query("SELECT s FROM StreamingSession s WHERE s.status = 'active' AND s.startedAt < :threshold")
    List<StreamingSession> findLongRunningSessions(@Param("threshold") LocalDateTime threshold);
    
    @Modifying
    @Query("UPDATE StreamingSession s SET s.status = :status, s.endedAt = :endedAt WHERE s.sessionId = :sessionId")
    void endSession(@Param("sessionId") String sessionId, @Param("status") String status, @Param("endedAt") LocalDateTime endedAt);
    
    @Modifying
    @Query("UPDATE StreamingSession s SET s.sdpAnswer = :sdpAnswer WHERE s.sessionId = :sessionId")
    void updateSdpAnswer(@Param("sessionId") String sessionId, @Param("sdpAnswer") String sdpAnswer);
    
    @Query("SELECT COUNT(s) FROM StreamingSession s WHERE s.userId = :userId AND s.status = 'active'")
    long countActiveSessionsByUser(@Param("userId") UUID userId);
}

// VoiceGenerationRepository.java
package com.virtualcompanion.mediaservice.repository;

import com.virtualcompanion.mediaservice.entity.VoiceGeneration;
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
public interface VoiceGenerationRepository extends JpaRepository<VoiceGeneration, UUID> {
    
    Optional<VoiceGeneration> findByTextHash(String textHash);
    
    Page<VoiceGeneration> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    Page<VoiceGeneration> findByCharacterIdOrderByCreatedAtDesc(UUID characterId, Pageable pageable);
    
    @Query("SELECT v FROM VoiceGeneration v WHERE v.userId = :userId AND v.characterId = :characterId AND v.textHash = :textHash AND v.status = 'completed'")
    Optional<VoiceGeneration> findCachedGeneration(@Param("userId") UUID userId, 
                                                   @Param("characterId") UUID characterId, 
                                                   @Param("textHash") String textHash);
    
    @Query("SELECT SUM(v.cost) FROM VoiceGeneration v WHERE v.userId = :userId AND v.createdAt >= :startDate")
    Double getTotalCostByUserSince(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(v) FROM VoiceGeneration v WHERE v.userId = :userId AND v.createdAt >= :startDate")
    Long countGenerationsByUserSince(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT v.provider, COUNT(v) FROM VoiceGeneration v WHERE v.createdAt >= :startDate GROUP BY v.provider")
    List<Object[]> getProviderUsageStats(@Param("startDate") LocalDateTime startDate);
    
    @Modifying
    @Query("UPDATE VoiceGeneration v SET v.status = :status, v.errorMessage = :errorMessage WHERE v.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status, @Param("errorMessage") String errorMessage);
    
    List<VoiceGeneration> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);
}

// Custom Repository for Complex Queries
// MediaFileRepositoryCustom.java
package com.virtualcompanion.mediaservice.repository;

import com.virtualcompanion.mediaservice.dto.MediaSearchRequest;
import com.virtualcompanion.mediaservice.entity.MediaFile;
import org.springframework.data.domain.Page;

public interface MediaFileRepositoryCustom {
    Page<MediaFile> searchMediaFiles(MediaSearchRequest request);
}

// MediaFileRepositoryCustomImpl.java
package com.virtualcompanion.mediaservice.repository;

import com.virtualcompanion.mediaservice.dto.MediaSearchRequest;
import com.virtualcompanion.mediaservice.entity.MediaFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MediaFileRepositoryCustomImpl implements MediaFileRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Page<MediaFile> searchMediaFiles(MediaSearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MediaFile> query = cb.createQuery(MediaFile.class);
        Root<MediaFile> root = query.from(MediaFile.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Add search criteria
        if (request.getUserId() != null) {
            predicates.add(cb.equal(root.get("userId"), request.getUserId()));
        }
        
        if (request.getCharacterId() != null) {
            predicates.add(cb.equal(root.get("characterId"), request.getCharacterId()));
        }
        
        if (request.getConversationId() != null) {
            predicates.add(cb.equal(root.get("conversationId"), request.getConversationId()));
        }
        
        if (request.getContentTypes() != null && !request.getContentTypes().isEmpty()) {
            List<Predicate> contentTypePredicates = new ArrayList<>();
            for (String contentType : request.getContentTypes()) {
                contentTypePredicates.add(cb.like(root.get("contentType"), contentType + "%"));
            }
            predicates.add(cb.or(contentTypePredicates.toArray(new Predicate[0])));
        }
        
        if (request.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getStartDate()));
        }
        
        if (request.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getEndDate()));
        }
        
        if (request.getProcessingStatus() != null) {
            predicates.add(cb.equal(root.get("processingStatus"), request.getProcessingStatus()));
        }
        
        if (request.getIsPublic() != null) {
            predicates.add(cb.equal(root.get("isPublic"), request.getIsPublic()));
        }
        
        // Always exclude deleted files
        predicates.add(cb.isNull(root.get("deletedAt")));
        
        query.where(predicates.toArray(new Predicate[0]));
        
        // Apply sorting
        if (request.getSortBy() != null) {
            Path<Object> sortPath = root.get(request.getSortBy());
            if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
                query.orderBy(cb.desc(sortPath));
            } else {
                query.orderBy(cb.asc(sortPath));
            }
        } else {
            query.orderBy(cb.desc(root.get("createdAt")));
        }
        
        // Get total count
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<MediaFile> countRoot = countQuery.from(MediaFile.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();
        
        // Apply pagination
        TypedQuery<MediaFile> typedQuery = entityManager.createQuery(query);
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        
        List<MediaFile> results = typedQuery.getResultList();
        
        return new PageImpl<>(results, PageRequest.of(page, size), totalElements);
    }
}