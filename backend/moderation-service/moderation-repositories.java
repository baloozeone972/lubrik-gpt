// ModerationRequestRepository.java
package com.virtualcompanion.moderationservice.repository;

import com.virtualcompanion.moderationservice.entity.ModerationRequest;
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

// ModerationResultRepository.java
package com.virtualcompanion.moderationservice.repository;

import com.virtualcompanion.moderationservice.entity.ModerationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModerationResultRepository extends JpaRepository<ModerationResult, UUID> {
    
    Optional<ModerationResult> findByModerationRequestId(UUID requestId);
    
    List<ModerationResult> findByModeratorIdAndCreatedAtBetween(UUID moderatorId, 
                                                                LocalDateTime start, 
                                                                LocalDateTime end);
    
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (mr.createdAt - req.createdAt))) FROM ModerationResult mr " +
           "JOIN mr.moderationRequest req WHERE mr.createdAt >= :since")
    Double getAverageResponseTime(@Param("since") LocalDateTime since);
    
    @Query("SELECT mr.decision, COUNT(mr) FROM ModerationResult mr WHERE mr.createdAt >= :since " +
           "GROUP BY mr.decision")
    List<Object[]> getDecisionDistribution(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(mr) FROM ModerationResult mr WHERE mr.decisionType = 'automated' " +
           "AND mr.createdAt >= :since")
    Long countAutomatedDecisions(@Param("since") LocalDateTime since);
}

// ModerationRuleRepository.java
package com.virtualcompanion.moderationservice.repository;

import com.virtualcompanion.moderationservice.entity.ModerationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModerationRuleRepository extends JpaRepository<ModerationRule, UUID> {
    
    List<ModerationRule> findByContentTypeAndIsActiveTrue(String contentType);
    
    List<ModerationRule> findByCategoryAndIsActiveTrue(String category);
    
    @Query("SELECT mr FROM ModerationRule mr WHERE mr.jurisdiction = :jurisdiction " +
           "AND mr.isActive = true ORDER BY mr.priority DESC")
    List<ModerationRule> findActiveRulesByJurisdiction(@Param("jurisdiction") String jurisdiction);
    
    @Query("SELECT mr FROM ModerationRule mr WHERE mr.ruleType = :type AND mr.isActive = true")
    List<ModerationRule> findActiveRulesByType(@Param("type") String type);
    
    boolean existsByNameAndIsActiveTrue(String name);
}

// BlockedContentRepository.java
package com.virtualcompanion.moderationservice.repository;

import com.virtualcompanion.moderationservice.entity.BlockedContent;
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
public interface BlockedContentRepository extends JpaRepository<BlockedContent, UUID> {
    
    Optional<BlockedContent> findByContentHash(String contentHash);
    
    Page<BlockedContent> findByBlockedByOrderByBlockedAtDesc(UUID blockedBy, Pageable pageable);
    
    Page<BlockedContent> findByReasonOrderByBlockedAtDesc(String reason, Pageable pageable);
    
    @Query("SELECT bc FROM BlockedContent bc WHERE bc.expiresAt IS NOT NULL AND bc.expiresAt <= :now")
    List<BlockedContent> findExpiredBlocks(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM BlockedContent bc WHERE bc.expiresAt <= :now")
    void deleteExpiredBlocks(@Param("now") LocalDateTime now);
    
    boolean existsByContentHash(String contentHash);
}

// UserModerationHistoryRepository.java
package com.virtualcompanion.moderationservice.repository;

import com.virtualcompanion.moderationservice.entity.UserModerationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
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

// AgeVerificationRepository.java
package com.virtualcompanion.moderationservice.repository;

import com.virtualcompanion.moderationservice.entity.AgeVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
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

// ContentReportRepository.java
package com.virtualcompanion.moderationservice.repository;

import com.virtualcompanion.moderationservice.entity.ContentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
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

// AppealRepository.java
package com.virtualcompanion.moderationservice.repository;

import com.virtualcompanion.moderationservice.entity.Appeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppealRepository extends JpaRepository<Appeal, UUID> {
    
    Optional<Appeal> findByDecisionIdAndUserId(UUID decisionId, UUID userId);
    
    Page<Appeal> findByUserIdOrderBySubmittedAtDesc(UUID userId, Pageable pageable);
    
    Page<Appeal> findByStatusOrderBySubmittedAtAsc(String status, Pageable pageable);
    
    @Query("SELECT a FROM Appeal a WHERE a.status = 'submitted' AND a.reviewDeadline <= :deadline")
    List<Appeal> findApproachingDeadline(@Param("deadline") LocalDateTime deadline);
    
    @Query("SELECT COUNT(a) FROM Appeal a WHERE a.status = 'overturned' AND a.reviewedAt >= :since")
    Long countOverturnedAppealsSince(@Param("since") LocalDateTime since);
}

// ModerationMetricsRepository.java
package com.virtualcompanion.moderationservice.repository;

import com.virtualcompanion.moderationservice.entity.ModerationMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModerationMetricsRepository extends JpaRepository<ModerationMetrics, UUID> {
    
    Optional<ModerationMetrics> findByMetricDate(LocalDate date);
    
    List<ModerationMetrics> findByMetricDateBetweenOrderByMetricDate(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(mm.totalContentReviewed), SUM(mm.automatedDecisions), SUM(mm.humanDecisions) " +
           "FROM ModerationMetrics mm WHERE mm.metricDate BETWEEN :startDate AND :endDate")
    Object[] getAggregatedMetrics(@Param("startDate") LocalDate startDate, 
                                 @Param("endDate") LocalDate endDate);
}