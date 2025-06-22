package com.virtualcompanion.conversationservice.entity;

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
