package com.virtualcompanion.moderationservice.repository;

public interface UserModerationHistoryRepository extends JpaRepository<UserModerationHistory, UUID> {
    
    Page<UserModerationHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    @Query("SELECT umh FROM UserModerationHistory umh WHERE umh.userId = :userId " +
           "AND umh.action IN :actions AND umh.createdAt >= :since")
    List<UserModerationHistory> findUserViolations(@Param("userId") UUID userId,
                                                   @Param("actions") List<String> actions,
                                                   @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(umh) FROM UserModerationHistory umh WHERE umh.userId = :userId " +
           "AND umh.action = :action AND umh.createdAt >= :since")
    Long countUserActionsSince(@Param("userId") UUID userId, 
                              @Param("action") String action,
                              @Param("since") LocalDateTime since);
    
    @Query("SELECT umh.action, COUNT(umh) FROM UserModerationHistory umh " +
           "WHERE umh.userId = :userId GROUP BY umh.action")
    List<Object[]> getUserActionSummary(@Param("userId") UUID userId);
}
