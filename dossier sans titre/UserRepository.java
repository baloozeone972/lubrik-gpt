package com.virtualcompanion.userservice.repository;

public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    
    @Query("SELECT u FROM User u WHERE u.email = :email OR u.username = :username")
    Optional<User> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);
    
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.createdAt BETWEEN :startDate AND :endDate")
    Page<User> findByStatusAndCreatedAtBetween(
        @Param("status") UserStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND u.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndCreatedAtBetween(
        @Param("status") UserStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt, u.lastLoginIp = :lastLoginIp WHERE u.id = :userId")
    void updateLastLogin(
        @Param("userId") UUID userId,
        @Param("lastLoginAt") LocalDateTime lastLoginAt,
        @Param("lastLoginIp") String lastLoginIp
    );
    
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.emailVerified = false AND u.createdAt < :cutoffDate")
    List<User> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil < :now")
    List<User> findLockedUsersToUnlock(@Param("now") LocalDateTime now);
}
