package com.virtualcompanion.moderationservice.repository;

public interface BlockedContentRepository extends JpaRepository<BlockedContent, UUID> {
    
    Optional<BlockedContent> findByContentHash(String contentHash);
    
    Page<BlockedContent> findByBlockedByOrderByBlockedAtDesc(UUID blockedBy, Pageable pageable);
    
    Page<BlockedContent> findByReasonOrderByBlockedAtDesc(String reason, Pageable pageable);
    
    @Query("SELECT bc FROM BlockedContent bc WHERE bc.expiresAt IS NOT NULL AND bc.expiresAt <= :now")
    List<BlockedContent> findExpiredBlocks(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM BlockedContent bc WHERE bc.expiresAt <= :now")
    void deleteExpiredBlocks(@Param("now") LocalDateTime now);
    
    boolean existsByContentHash(String contentHash);
}
