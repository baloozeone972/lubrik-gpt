package com.virtualcompanion.userservice.repository;

import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.entity.UserCompliance;
import com.virtualcompanion.userservice.entity.UserSession;
import com.virtualcompanion.userservice.entity.VerificationToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.emailVerified = true")
    Page<User> findAllActiveUsers(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.subscriptionLevel = :level")
    List<User> findBySubscriptionLevel(@Param("level") User.SubscriptionLevel level);
    
    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findUsersRegisteredAfter(@Param("date") LocalDateTime date);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
    
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("lastLogin") LocalDateTime lastLogin);
    
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    void verifyEmail(@Param("userId") UUID userId);
    
    @Query("SELECT u FROM User u WHERE u.twoFactorEnabled = true")
    List<User> findUsersWithTwoFactor();
    
    @Query(value = "SELECT * FROM users WHERE age >= :minAge", nativeQuery = true)
    List<User> findAdultUsers(@Param("minAge") int minAge);
}

@Repository
interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    List<UserSession> findByUserIdAndIsActiveTrue(UUID userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now AND s.isActive = true")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId")
    void invalidateAllUserSessions(@Param("userId") UUID userId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.sessionToken = :token")
    void invalidateSession(@Param("token") String token);
    
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.isActive = true")
    long countActiveSessionsForUser(@Param("userId") UUID userId);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}

@Repository
interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {
    
    Optional<UserPreference> findByUserId(UUID userId);
    
    @Query("SELECT p FROM UserPreference p WHERE p.theme = :theme")
    List<UserPreference> findByTheme(@Param("theme") String theme);
    
    @Modifying
    @Query("UPDATE UserPreference p SET p.language = :language WHERE p.userId = :userId")
    void updateLanguage(@Param("userId") UUID userId, @Param("language") String language);
}

@Repository
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

@Repository
interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    
    Optional<VerificationToken> findByToken(String token);
    
    List<VerificationToken> findByUserIdAndUsedFalse(UUID userId);
    
    @Query("SELECT t FROM VerificationToken t WHERE t.expiresAt < :now AND t.used = false")
    List<VerificationToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE VerificationToken t SET t.used = true, t.usedAt = :usedAt WHERE t.token = :token")
    void markAsUsed(@Param("token") String token, @Param("usedAt") LocalDateTime usedAt);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    @Query("SELECT COUNT(t) FROM VerificationToken t WHERE t.userId = :userId AND t.type = :type AND t.used = false AND t.expiresAt > :now")
    long countActiveTokensForUser(@Param("userId") UUID userId, @Param("type") VerificationToken.TokenType type, @Param("now") LocalDateTime now);
}