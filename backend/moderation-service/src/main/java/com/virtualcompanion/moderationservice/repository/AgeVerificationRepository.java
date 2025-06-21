package com.virtualcompanion.moderationservice.repository;

public interface AgeVerificationRepository extends JpaRepository<AgeVerification, UUID> {
    
    Optional<AgeVerification> findByUserIdAndStatusAndExpiresAtAfter(UUID userId, String status, LocalDateTime now);
    
    Optional<AgeVerification> findTopByUserIdOrderByVerifiedAtDesc(UUID userId);
    
    @Query("SELECT av FROM AgeVerification av WHERE av.userId = :userId AND av.status = 'verified' " +
           "AND (av.expiresAt IS NULL OR av.expiresAt > :now)")
    Optional<AgeVerification> findValidVerification(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE AgeVerification av SET av.status = :status, av.failureReason = :reason " +
           "WHERE av.id = :id")
    void updateVerificationStatus(@Param("id") UUID id, @Param("status") String status, 
                                 @Param("reason") String reason);
}
