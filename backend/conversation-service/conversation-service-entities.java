// ConversationServiceApplication.java
package com.virtualcompanion.conversationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableReactiveMongoRepositories
public class ConversationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConversationServiceApplication.class, args);
    }
}

// ===== ENTITIES (JPA - PostgreSQL) =====

// Conversation.java
package com.virtualcompanion.conversationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(n = "conversations", indexes = {
    @Index(n = "idx_conversation_user", columnList = "user_id"),
    @Index(n = "idx_conversation_character", columnList = "character_id"),
    @Index(n = "idx_conversation_created", columnList = "created_at"),
    @Index(n = "idx_conversation_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "character_id", nullable = false)
    private UUID characterId;
    
    @Column(n = "title")
    private String title;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationStatus status = ConversationStatus.ACTIVE;
    
    @Column(n = "message_count")
    private Integer messageCount = 0;
    
    @Column(n = "user_message_count")
    private Integer userMessageCount = 0;
    
    @Column(n = "character_message_count")
    private Integer characterMessageCount = 0;
    
    @Column(n = "total_tokens_used")
    private Long totalTokensUsed = 0L;
    
    @Column(n = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    @Column(n = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;
    
    @Column(n = "emotional_state")
    private String emotionalState;
    
    @Column(n = "relationship_score")
    private Integer relationshipScore = 50;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<ConversationMemory> memories = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(n = "conversation_tags", 
        joinColumns = @JoinColumn(n = "conversation_id"))
    @Column(n = "tag")
    private Set<String> tags = new HashSet<>();
    
    @Column(n = "language_code")
    private String languageCode = "en";
    
    @Column(n = "is_favorite")
    private boolean isFavorite = false;
    
    @Column(n = "is_archived")
    private boolean isArchived = false;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// ConversationStatus.java
package com.virtualcompanion.conversationservice.entity;

public enum ConversationStatus {
    ACTIVE,
    PAUSED,
    ENDED,
    ARCHIVED,
    DELETED
}

// ConversationMemory.java
package com.virtualcompanion.conversationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "conversation_memories", indexes = {
    @Index(n = "idx_memory_conversation", columnList = "conversation_id"),
    @Index(n = "idx_memory_importance", columnList = "importance_score"),
    @Index(n = "idx_memory_type", columnList = "memory_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ConversationMemory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @Column(n = "memory_type", nullable = false)
    private String memoryType;
    
    @Column(n = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(n = "embedding_vector", columnDefinition = "vector(1536)")
    private float[] embeddingVector;
    
    @Column(n = "importance_score")
    private Double importanceScore = 0.5;
    
    @Column(n = "emotional_context")
    private String emotionalContext;
    
    @Column(n = "referenced_count")
    private Integer referencedCount = 0;
    
    @Column(n = "last_referenced_at")
    private LocalDateTime lastReferencedAt;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}

// ===== ENTITIES (MongoDB - Messages) =====

// Message.java
package com.virtualcompanion.conversationservice.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(n = "conversation_user", def = "{'conversationId': 1, 'userId': 1}"),
    @CompoundIndex(n = "conversation_timestamp", def = "{'conversationId': 1, 'timestamp': -1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    private String id;
    
    @Indexed
    private UUID conversationId;
    
    @Indexed
    private UUID userId;
    
    private UUID characterId;
    
    private MessageType type;
    
    private String content;
    
    private MessageMetadata metadata;
    
    private List<MessageAttachment> attachments;
    
    private MessageAnalysis analysis;
    
    private Integer tokensUsed;
    
    private Long processingTimeMs;
    
    @Indexed
    private LocalDateTime timestamp;
    
    private LocalDateTime editedAt;
    
    private boolean isDeleted;
    
    private String deletedReason;
    
    public enum MessageType {
        USER_MESSAGE,
        CHARACTER_MESSAGE,
        SYSTEM_MESSAGE,
        ERROR_MESSAGE
    }
}

// MessageMetadata.java
package com.virtualcompanion.conversationservice.document;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMetadata {
    private String emotion;
    private Double sentimentScore;
    private String intentDetected;
    private Map<String, Object> contextVariables;
    private String voiceId;
    private String animationId;
    private Boolean isFilteredContent;
    private String filterReason;
}

// MessageAttachment.java
package com.virtualcompanion.conversationservice.document;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {
    private String type; // image, audio, file
    private String url;
    private String mimeType;
    private Long size;
    private String n;
    private Map<String, Object> metadata;
}

// MessageAnalysis.java
package com.virtualcompanion.conversationservice.document;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAnalysis {
    private List<String> topics;
    private List<String> entities;
    private Map<String, Double> emotionScores;
    private Double toxicityScore;
    private Boolean requiresModeration;
    private String moderationReason;
    private List<String> suggestedResponses;
}

// ConversationContext.java
package com.virtualcompanion.conversationservice.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "conversation_contexts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private UUID conversationId;
    
    private CharacterContext characterContext;
    
    private UserContext userContext;
    
    private List<ContextMemory> shortTermMemory;
    
    private List<ContextMemory> longTermMemory;
    
    private Map<String, Object> sessionVariables;
    
    private String currentTopic;
    
    private List<String> topicHistory;
    
    private RelationshipState relationshipState;
    
    private LocalDateTime lastUpdated;
}

// CharacterContext.java
package com.virtualcompanion.conversationservice.document;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterContext {
    private UUID characterId;
    private String characterN;
    private Map<String, Object> personality;
    private String currentMood;
    private Map<String, Double> moodHistory;
    private String preferredResponseStyle;
    private Map<String, Object> customSettings;
}

// UserContext.java
package com.virtualcompanion.conversationservice.document;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private UUID userId;
    private String preferredN;
    private List<String> interests;
    private Map<String, Object> preferences;
    private String communicationStyle;
    private List<String> triggerTopics;
    private Map<String, LocalDateTime> importantDates;
}

// ContextMemory.java
package com.virtualcompanion.conversationservice.document;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextMemory {
    private String id;
    private String type;
    private String content;
    private Double importance;
    private LocalDateTime timestamp;
    private Integer accessCount;
    private Map<String, Object> metadata;
}

// RelationshipState.java
package com.virtualcompanion.conversationservice.document;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipState {
    private Integer trustLevel;
    private Integer intimacyLevel;
    private Integer engagementLevel;
    private Map<String, Integer> emotionalBonds;
    private Map<String, LocalDateTime> milestones;
    private Integer overallScore;
}

// ===== STREAMING ENTITIES =====

// StreamingSession.java
package com.virtualcompanion.conversationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "streaming_sessions", indexes = {
    @Index(n = "idx_streaming_conversation", columnList = "conversation_id"),
    @Index(n = "idx_streaming_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "conversation_id", nullable = false)
    private UUID conversationId;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "session_token", unique = true, nullable = false)
    private String sessionToken;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamingStatus status = StreamingStatus.CONNECTING;
    
    @Column(n = "connection_type")
    private String connectionType; // websocket, sse
    
    @Column(n = "client_ip")
    private String clientIp;
    
    @Column(n = "user_agent")
    private String userAgent;
    
    @Column(n = "connected_at")
    @CreationTimestamp
    private LocalDateTime connectedAt;
    
    @Column(n = "disconnected_at")
    private LocalDateTime disconnectedAt;
    
    @Column(n = "last_activity_at")
    private LocalDateTime lastActivityAt;
    
    @Column(n = "messages_sent")
    private Integer messagesSent = 0;
    
    @Column(n = "messages_received")
    private Integer messagesReceived = 0;
    
    public enum StreamingStatus {
        CONNECTING,
        CONNECTED,
        ACTIVE,
        IDLE,
        DISCONNECTED,
        ERROR
    }
}

// ConversationAnalytics.java
package com.virtualcompanion.conversationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "conversation_analytics", indexes = {
    @Index(n = "idx_analytics_conversation", columnList = "conversation_id", unique = true),
    @Index(n = "idx_analytics_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "conversation_id", nullable = false, unique = true)
    private UUID conversationId;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "character_id", nullable = false)
    private UUID characterId;
    
    // Engagement Metrics
    @Column(n = "total_duration_seconds")
    private Long totalDurationSeconds = 0L;
    
    @Column(n = "average_response_time_ms")
    private Long averageResponseTimeMs;
    
    @Column(n = "user_engagement_score")
    private Double userEngagementScore;
    
    @Column(n = "character_performance_score")
    private Double characterPerformanceScore;
    
    // Sentiment Analysis
    @Column(n = "average_sentiment_score")
    private Double averageSentimentScore;
    
    @Column(n = "positive_message_ratio")
    private Double positiveMessageRatio;
    
    @Column(n = "negative_message_ratio")
    private Double negativeMessageRatio;
    
    // Content Analysis
    @Column(n = "topics_discussed", columnDefinition = "JSON")
    private String topicsDiscussed;
    
    @Column(n = "vocabulary_diversity_score")
    private Double vocabularyDiversityScore;
    
    @Column(n = "conversation_depth_score")
    private Double conversationDepthScore;
    
    // User Satisfaction
    @Column(n = "user_satisfaction_score")
    private Integer userSatisfactionScore;
    
    @Column(n = "would_recommend")
    private Boolean wouldRecommend;
    
    @Column(n = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;
    
    @Column(n = "last_analyzed_at")
    private LocalDateTime lastAnalyzedAt;
}