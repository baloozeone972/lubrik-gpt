package com.virtualcompanion.moderationservice.repository;

public interface AppealRepository extends JpaRepository<Appeal, UUID> {
    
    Optional<Appeal> findByDecisionIdAndUserId(UUID decisionId, UUID userId);
    
    Page<Appeal> findByUserIdOrderBySubmittedAtDesc(UUID userId, Pageable pageable);
    
    Page<Appeal> findByStatusOrderBySubmittedAtAsc(String status, Pageable pageable);
    
    @Query("SELECT a FROM Appeal a WHERE a.status = 'submitted' AND a.reviewDeadline <= :deadline")
    List<Appeal> findApproachingDeadline(@Param("deadline") LocalDateTime deadline);
    
    @Query("SELECT COUNT(a) FROM Appeal a WHERE a.status = 'overturned' AND a.reviewedAt >= :since")
    Long countOverturnedAppealsSince(@Param("since") LocalDateTime since);
}
