// MediaUploadRequest.java
package com.virtualcompanion.mediaservice.dto;

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
public class MediaUploadRequest {
    
    private UUID characterId;
    
    private UUID conversationId;
    
    @NotNull(message = "Media type is required")
    private String mediaType; // video, audio, image
    
    private String title;
    
    private String description;
    
    private Boolean isPublic;
    
    private Map<String, Object> metadata;
}

// MediaUploadResponse.java
package com.virtualcompanion.mediaservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaUploadResponse {
    
    private UUID id;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String url;
    private String thumbnailUrl;
    private String processingStatus;
    private Double duration;
    private Integer width;
    private Integer height;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}

// TranscodeRequest.java
package com.virtualcompanion.mediaservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscodeRequest {
    
    @NotNull(message = "Quality preset is required")
    private String qualityPreset; // low, medium, high, custom
    
    private String format; // mp4, webm, etc.
    
    private TranscodeSettings customSettings;
    
    private List<String> additionalFormats;
    
    private Boolean generateThumbnails;
    
    private Integer thumbnailCount;
}

// TranscodeSettings.java
package com.virtualcompanion.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscodeSettings {
    
    private String videoCodec;
    private String audioCodec;
    private String bitrate;
    private String resolution;
    private Integer fps;
    private String audioSampleRate;
    private Integer audioBitrate;
}

// TranscodeResponse.java
package com.virtualcompanion.mediaservice.dto;

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
public class TranscodeResponse {
    
    private UUID jobId;
    private String status; // queued, processing, completed, failed
    private Integer progress;
    private List<MediaVariant> variants;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}

// MediaVariant.java
package com.virtualcompanion.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaVariant {
    
    private String variantType;
    private String quality;
    private String format;
    private String url;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer bitrate;
}

// StreamingRequest.java
package com.virtualcompanion.mediaservice.dto;

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
public class StreamingRequest {
    
    @NotNull(message = "Character ID is required")
    private UUID characterId;
    
    private UUID conversationId;
    
    @NotNull(message = "Stream type is required")
    private String streamType; // video, audio, screen
    
    private StreamingConfig config;
    
    private Map<String, Object> metadata;
}

// StreamingConfig.java
package com.virtualcompanion.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingConfig {
    
    private Integer maxVideoBitrate;
    private Integer maxAudioBitrate;
    private String videoCodec;
    private String audioCodec;
    private Boolean enableRecording;
    private Boolean enableNoiseSuppression;
    private Boolean enableEchoCancellation;
}

// StreamingResponse.java
package com.virtualcompanion.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingResponse {
    
    private String sessionId;
    private String kurentoSessionId;
    private String sdpOffer;
    private List<IceCandidate> iceCandidates;
    private String status;
    private LocalDateTime startedAt;
}

// IceCandidate.java
package com.virtualcompanion.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IceCandidate {
    
    private String candidate;
    private String sdpMLineIndex;
    private String sdpMid;
}

// VoiceGenerationRequest.java
package com.virtualcompanion.mediaservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class VoiceGenerationRequest {
    
    @NotNull(message = "Character ID is required")
    private UUID characterId;
    
    private UUID conversationId;
    
    @NotBlank(message = "Text content is required")
    @Size(max = 5000, message = "Text cannot exceed 5000 characters")
    private String text;
    
    private String provider; // elevenlabs, azure, google
    
    private String voiceId;
    
    private VoiceSettings settings;
    
    private String outputFormat; // mp3, wav, ogg
    
    private Map<String, Object> metadata;
}

// VoiceSettings.java
package com.virtualcompanion.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSettings {
    
    private Double speed; // 0.5 - 2.0
    private Double pitch; // -20 to 20
    private Double volume; // 0.0 - 1.0
    private String emotion; // neutral, happy, sad, angry, etc.
    private Double emotionIntensity; // 0.0 - 1.0
    private String language;
    private String style; // chat, narration, customerservice
}

// VoiceGenerationResponse.java
package com.virtualcompanion.mediaservice.dto;

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
public class VoiceGenerationResponse {
    
    private UUID id;
    private String audioUrl;
    private String format;
    private Long fileSize;
    private Double duration;
    private String provider;
    private String voiceId;
    private String status;
    private Double cost;
    private LocalDateTime createdAt;
}

// MediaSearchRequest.java
package com.virtualcompanion.mediaservice.dto;

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
public class MediaSearchRequest {
    
    private UUID userId;
    private UUID characterId;
    private UUID conversationId;
    private List<String> mediaTypes;
    private List<String> contentTypes;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String processingStatus;
    private Boolean isPublic;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;
}

// MediaStatisticsResponse.java
package com.virtualcompanion.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaStatisticsResponse {
    
    private Long totalFiles;
    private Long totalSize;
    private Map<String, Long> filesByType;
    private Map<String, Long> sizeByType;
    private Long totalDuration;
    private Long totalTranscodes;
    private Long totalVoiceGenerations;
    private Double totalVoiceCost;
    private Map<String, Long> voiceGenerationsByProvider;
}

// MediaProcessingEvent.java
package com.virtualcompanion.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaProcessingEvent {
    
    private String eventType; // upload_started, processing, completed, failed
    private UUID mediaId;
    private UUID userId;
    private String processingType;
    private Integer progress;
    private String status;
    private String message;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}