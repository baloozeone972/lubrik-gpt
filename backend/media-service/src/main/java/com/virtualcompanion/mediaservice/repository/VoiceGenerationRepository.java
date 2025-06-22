package com.virtualcompanion.mediaservice.repository;

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
