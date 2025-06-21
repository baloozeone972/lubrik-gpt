// ConversationRepository.java
package com.virtualcompanion.conversationservice.repository;

import com.virtualcompanion.conversationservice.entity.Conversation;
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
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);
    
    Page<Conversation> findByUserIdOrderByLastActivityAtDesc(UUID userId, Pageable pageable);
    
    Page<Conversation> findByUserIdAndCharacterIdOrderByLastActivityAtDesc(UUID userId, UUID characterId, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId AND c.status = :status")
    List<Conversation> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);
    
    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId AND c.lastActivityAt >= :since")
    List<Conversation> findRecentConversations(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    @Modifying
    @Query("UPDATE Conversation c SET c.lastActivityAt = :now, c.messageCount = c.messageCount + 1 WHERE c.id = :id")
    void updateActivity(@Param("id") UUID id, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Conversation c SET c.status = :status, c.endedAt = :endedAt WHERE c.id = :id")
    void endConversation(@Param("id") UUID id, @Param("status") String status, @Param("endedAt") LocalDateTime endedAt);
    
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.userId = :userId AND c.characterId = :characterId")
    long countUserCharacterConversations(@Param("userId") UUID userId, @Param("characterId") UUID characterId);
    
    @Query("SELECT AVG(c.messageCount) FROM Conversation c WHERE c.userId = :userId")
    Double getAverageMessageCountByUser(@Param("userId") UUID userId);
    
    @Query("SELECT c FROM Conversation c WHERE c.status = 'active' AND c.lastActivityAt < :threshold")
    List<Conversation> findInactiveConversations(@Param("threshold") LocalDateTime threshold);
}

// ConversationMemoryRepository.java
package com.virtualcompanion.conversationservice.repository;

import com.virtualcompanion.conversationservice.entity.ConversationMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMemoryRepository extends JpaRepository<ConversationMemory, UUID> {
    
    Optional<ConversationMemory> findByConversationIdAndUserId(UUID conversationId, UUID userId);
    
    List<ConversationMemory> findByUserIdAndCharacterIdOrderByLastUpdatedDesc(UUID userId, UUID characterId);
    
    @Query(value = "SELECT * FROM conversation_memories WHERE user_id = :userId AND character_id = :characterId " +
                   "ORDER BY embedding <-> :queryEmbedding LIMIT :limit", nativeQuery = true)
    List<ConversationMemory> findSimilarMemories(@Param("userId") UUID userId, 
                                                 @Param("characterId") UUID characterId,
                                                 @Param("queryEmbedding") float[] queryEmbedding,
                                                 @Param("limit") int limit);
    
    @Query("SELECT cm FROM ConversationMemory cm WHERE cm.userId = :userId AND cm.characterId = :characterId " +
           "AND cm.memoryType = :type ORDER BY cm.importance DESC")
    List<ConversationMemory> findByTypeAndImportance(@Param("userId") UUID userId,
                                                     @Param("characterId") UUID characterId,
                                                     @Param("type") String type);
    
    void deleteByConversationId(UUID conversationId);
}

// StreamingSessionRepository.java
package com.virtualcompanion.conversationservice.repository;

import com.virtualcompanion.conversationservice.entity.StreamingSession;
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
public interface StreamingSessionRepository extends JpaRepository<StreamingSession, UUID> {
    
    Optional<StreamingSession> findBySessionIdAndUserId(String sessionId, UUID userId);
    
    List<StreamingSession> findByUserIdAndIsActiveTrue(UUID userId);
    
    List<StreamingSession> findByConversationIdAndIsActiveTrue(UUID conversationId);
    
    @Modifying
    @Query("UPDATE StreamingSession s SET s.isActive = false, s.disconnectedAt = :now WHERE s.sessionId = :sessionId")
    void closeSession(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE StreamingSession s SET s.lastPingAt = :now WHERE s.sessionId = :sessionId")
    void updatePing(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM StreamingSession s WHERE s.isActive = true AND s.lastPingAt < :threshold")
    List<StreamingSession> findStaleSessions(@Param("threshold") LocalDateTime threshold);
    
    void deleteByIsActiveFalseAndDisconnectedAtBefore(LocalDateTime threshold);
}

// ConversationAnalyticsRepository.java
package com.virtualcompanion.conversationservice.repository;

import com.virtualcompanion.conversationservice.entity.ConversationAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationAnalyticsRepository extends JpaRepository<ConversationAnalytics, UUID> {
    
    Optional<ConversationAnalytics> findByConversationId(UUID conversationId);
    
    @Query("SELECT ca FROM ConversationAnalytics ca WHERE ca.userId = :userId " +
           "AND ca.createdAt >= :startDate AND ca.createdAt <= :endDate")
    List<ConversationAnalytics> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT AVG(ca.sentimentScore) FROM ConversationAnalytics ca WHERE ca.userId = :userId")
    Double getAverageSentimentByUser(@Param("userId") UUID userId);
    
    @Query("SELECT ca.dominantEmotion, COUNT(ca) FROM ConversationAnalytics ca " +
           "WHERE ca.userId = :userId GROUP BY ca.dominantEmotion")
    List<Object[]> getEmotionDistributionByUser(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(ca.responseTime) FROM ConversationAnalytics ca WHERE ca.characterId = :characterId")
    Double getAverageResponseTimeByCharacter(@Param("characterId") UUID characterId);
    
    @Query("SELECT DATE(ca.createdAt), COUNT(ca), AVG(ca.engagementScore) " +
           "FROM ConversationAnalytics ca WHERE ca.userId = :userId " +
           "GROUP BY DATE(ca.createdAt) ORDER BY DATE(ca.createdAt) DESC")
    List<Object[]> getDailyEngagementMetrics(@Param("userId") UUID userId);
}

// MongoDB Repositories

// MessageRepository.java
package com.virtualcompanion.conversationservice.repository;

import com.virtualcompanion.conversationservice.document.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    
    Page<Message> findByConversationIdOrderByTimestampDesc(UUID conversationId, Pageable pageable);
    
    List<Message> findByConversationIdAndTimestampBetween(UUID conversationId, 
                                                          LocalDateTime start, 
                                                          LocalDateTime end);
    
    @Query("{ 'conversationId': ?0, 'metadata.emotion': { $exists: true } }")
    List<Message> findMessagesWithEmotions(UUID conversationId);
    
    @Query("{ 'conversationId': ?0, 'role': 'assistant', 'metadata.processingTime': { $gt: ?1 } }")
    List<Message> findSlowResponses(UUID conversationId, Long threshold);
    
    Long countByConversationId(UUID conversationId);
    
    @Query(value = "{ 'conversationId': ?0 }", delete = true)
    void deleteByConversationId(UUID conversationId);
    
    @Query("{ 'userId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<Message> findUserMessagesByDateRange(UUID userId, LocalDateTime start, LocalDateTime end);
}

// ConversationContextRepository.java
package com.virtualcompanion.conversationservice.repository;

import com.virtualcompanion.conversationservice.document.ConversationContext;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationContextRepository extends MongoRepository<ConversationContext, String> {
    
    Optional<ConversationContext> findByConversationId(UUID conversationId);
    
    @Query("{ 'conversationId': ?0, 'isActive': true }")
    Optional<ConversationContext> findActiveContext(UUID conversationId);
    
    void deleteByConversationId(UUID conversationId);
}

// CharacterContextRepository.java
package com.virtualcompanion.conversationservice.repository;

import com.virtualcompanion.conversationservice.document.CharacterContext;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CharacterContextRepository extends MongoRepository<CharacterContext, String> {
    
    Optional<CharacterContext> findByUserIdAndCharacterId(UUID userId, UUID characterId);
    
    @Query("{ 'userId': ?0, 'characterId': ?1, 'isActive': true }")
    Optional<CharacterContext> findActiveCharacterContext(UUID userId, UUID characterId);
    
    void deleteByUserIdAndCharacterId(UUID userId, UUID characterId);
}