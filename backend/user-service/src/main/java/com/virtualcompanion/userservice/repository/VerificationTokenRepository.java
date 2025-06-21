package com.virtualcompanion.userservice.repository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    
    Optional<VerificationToken> findByToken(String token);
    
    Optional<VerificationToken> findByTokenAndTokenType(String token, TokenType tokenType);
    
    @Query("SELECT t FROM VerificationToken t WHERE t.user.id = :userId AND t.tokenType = :tokenType AND t.usedAt IS NULL AND t.expiresAt > :now")
    Optional<VerificationToken> findValidTokenByUserAndType(
        @Param("userId") UUID userId,
        @Param("tokenType") TokenType tokenType,
        @Param("now") LocalDateTime now
    );
    
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :now OR t.usedAt IS NOT NULL")
    void deleteExpiredAndUsedTokens(@Param("now") LocalDateTime now);
}
