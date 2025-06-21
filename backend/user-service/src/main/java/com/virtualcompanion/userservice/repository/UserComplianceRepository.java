package com.virtualcompanion.userservice.repository;

interface UserComplianceRepository extends JpaRepository<UserCompliance, UUID> {
    
    Optional<UserCompliance> findByUserId(UUID userId);
    
    @Query("SELECT c FROM UserCompliance c WHERE c.gdprConsentDate IS NOT NULL")
    List<UserCompliance> findUsersWithGdprConsent();
    
    @Query("SELECT c FROM UserCompliance c WHERE c.jurisdiction = :jurisdiction")
    List<UserCompliance> findByJurisdiction(@Param("jurisdiction") String jurisdiction);
    
    @Modifying
    @Query("UPDATE UserCompliance c SET c.ageVerificationDate = :date, c.ageVerificationMethod = :method WHERE c.userId = :userId")
    void updateAgeVerification(@Param("userId") UUID userId, @Param("date") LocalDateTime date, @Param("method") String method);
}
