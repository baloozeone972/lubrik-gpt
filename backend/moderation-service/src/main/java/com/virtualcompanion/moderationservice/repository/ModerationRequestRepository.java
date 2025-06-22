package com.virtualcompanion.moderationservice.repository;

public interface ModerationRequestRepository extends JpaRepository<ModerationRequest, UUID> {

    Optional<ModerationRequest> findByContentIdAndContentType(String contentId, String contentType);

    Page<ModerationRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<ModerationRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT mr FROM ModerationRequest mr WHERE mr.status = 'pending' AND mr.priority = :priority " +
            "ORDER BY mr.createdAt ASC")
    List<ModerationRequest> findPendingByPriority(@Param("priority") String priority, Pageable pageable);

    @Query("SELECT mr FROM ModerationRequest mr WHERE mr.status = 'flagged' AND mr.requiresHumanReview = true " +
            "ORDER BY mr.priority DESC, mr.createdAt ASC")
    Page<ModerationRequest> findRequiringHumanReview(Pageable pageable);

    @Query("SELECT COUNT(mr) FROM ModerationRequest mr WHERE mr.status = :status AND mr.createdAt >= :since")
    Long countByStatusSince(@Param("status") String status, @Param("since") LocalDateTime since);

    @Query("SELECT mr.violationCategory, COUNT(mr) FROM ModerationRequest mr " +
            "WHERE mr.status = 'rejected' AND mr.createdAt >= :since " +
            "GROUP BY mr.violationCategory")
    List<Object[]> getViolationStatistics(@Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE ModerationRequest mr SET mr.status = :status, mr.processedAt = :processedAt " +
            "WHERE mr.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status,
                      @Param("processedAt") LocalDateTime processedAt);

    List<ModerationRequest> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);
}
