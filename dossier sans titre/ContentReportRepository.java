package com.virtualcompanion.moderationservice.repository;

public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {
    
    Page<ContentReport> findByReporterIdOrderBySubmittedAtDesc(UUID reporterId, Pageable pageable);
    
    Page<ContentReport> findByStatusOrderByPriorityDescSubmittedAtAsc(String status, Pageable pageable);
    
    @Query("SELECT cr FROM ContentReport cr WHERE cr.contentId = :contentId AND cr.contentType = :contentType")
    List<ContentReport> findByContent(@Param("contentId") String contentId, 
                                     @Param("contentType") String contentType);
    
    @Query("SELECT cr.reason, COUNT(cr) FROM ContentReport cr WHERE cr.submittedAt >= :since " +
           "GROUP BY cr.reason ORDER BY COUNT(cr) DESC")
    List<Object[]> getReportReasonStatistics(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(DISTINCT cr.contentId) FROM ContentReport cr WHERE cr.status = 'resolved' " +
           "AND cr.resolution = 'content_removed' AND cr.resolvedAt >= :since")
    Long countContentRemovedSince(@Param("since") LocalDateTime since);
    
    @Modifying
    @Query("UPDATE ContentReport cr SET cr.status = :status, cr.resolution = :resolution, " +
           "cr.resolvedAt = :resolvedAt, cr.resolvedBy = :resolvedBy WHERE cr.id = :id")
    void resolveReport(@Param("id") UUID id, @Param("status") String status, 
                      @Param("resolution") String resolution, @Param("resolvedAt") LocalDateTime resolvedAt,
                      @Param("resolvedBy") UUID resolvedBy);
}
