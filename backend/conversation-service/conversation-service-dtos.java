// StartConversationRequest.java
package com.virtualcompanion.conversationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartConversationRequest {
    
    @NotNull(message = "Character ID is required")
    private UUID characterId;
    
    private String initialMessage;
    
    private String conversationMode; // text, voice, video
    
    private Map<String, Object> context;
    
    private ConversationSettings settings;
}

// ConversationSettings.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSettings {
    
    private Boolean saveHistory;
    private String language;
    private Double temperature;
    private Integer maxTokens;
    private String responseStyle; // casual, formal, romantic, friendly
    private Boolean enableEmotions;
    private Boolean enableActions;
}

// ConversationResponse.java
package com.virtualcompanion.conversationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationResponse {
    
    private UUID id;
    private UUID userId;
    private UUID characterId;
    private String characterName;
    private String status; // active, paused, ended
    private String mode;
    private LocalDateTime startedAt;
    private LocalDateTime lastActivityAt;
    private Long messageCount;
    private ConversationSettings settings;
    private List<MessageResponse> recentMessages;
}

// SendMessageRequest.java
package com.virtualcompanion.conversationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    @NotBlank(message = "Message content is required")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String content;
    
    private String messageType; // text, voice, image
    
    private Map<String, Object> metadata;
    
    private MessageOptions options;
}

// MessageOptions.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageOptions {
    
    private Boolean requestVoiceResponse;
    private Boolean includeEmotions;
    private Boolean includeActions;
    private String preferredTone;
    private Integer maxResponseLength;
}

// MessageResponse.java
package com.virtualcompanion.conversationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {
    
    private String id;
    private String role; // user, assistant
    private String content;
    private String messageType;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    private EmotionData emotion;
    private ActionData action;
    private String voiceUrl;
    private Long processingTime;
}

// EmotionData.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionData {
    
    private String primary;
    private Double intensity;
    private String expression;
    private Map<String, Double> emotionScores;
}

// ActionData.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionData {
    
    private String type; // gesture, movement, expression
    private String description;
    private Double duration;
    private Map<String, Object> parameters;
}

// ConversationHistoryRequest.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistoryRequest {
    
    private UUID characterId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer limit;
    private String sortOrder; // asc, desc
}

// ConversationHistoryResponse.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistoryResponse {
    
    private List<ConversationResponse> conversations;
    private Long totalConversations;
    private ConversationStatistics statistics;
}

// ConversationStatistics.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationStatistics {
    
    private Long totalMessages;
    private Double averageMessagesPerConversation;
    private Double averageConversationDuration;
    private Map<String, Long> messagesByType;
    private Map<String, Long> conversationsByMode;
    private Map<String, Double> emotionDistribution;
}

// StreamingMessageEvent.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingMessageEvent {
    
    private String eventType; // message_start, message_chunk, message_end, typing, error
    private String conversationId;
    private String messageId;
    private String content;
    private Integer chunkIndex;
    private Boolean isComplete;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}

// ConversationExportRequest.java
package com.virtualcompanion.conversationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationExportRequest {
    
    @NotNull
    private List<UUID> conversationIds;
    
    private String format; // json, txt, pdf
    
    private Boolean includeMetadata;
    
    private Boolean includeEmotions;
    
    private Boolean anonymize;
}

// ConversationExportResponse.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationExportResponse {
    
    private String exportId;
    private String downloadUrl;
    private Long fileSize;
    private String format;
    private LocalDateTime expiresAt;
    private Integer conversationCount;
}

// MemoryUpdateRequest.java
package com.virtualcompanion.conversationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryUpdateRequest {
    
    @NotNull
    private UUID characterId;
    
    private List<MemoryItem> memories;
    
    private Map<String, Object> context;
}

// MemoryItem.java
package com.virtualcompanion.conversationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryItem {
    
    private String type; // fact, preference, event, relationship
    private String content;
    private Double importance;
    private String category;
    private Map<String, Object> metadata;
}