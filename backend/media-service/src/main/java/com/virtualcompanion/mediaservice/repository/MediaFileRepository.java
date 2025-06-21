package com.virtualcompanion.mediaservice.repository;

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
