// TextModerationRequest.java
package com.virtualcompanion.moderationservice.dto;

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
public class TextModerationRequest {
    
    @NotBlank(message = "Text content is required")
    @Size(max = 5000, message = "Text cannot exceed 5000 characters")
    private String text;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    private UUID characterId;
    
    private UUID conversationId;
    
    private String contentType; // message, profile, character_description
    
    private String language;
    
    private Map<String, Object> context;
    
    private Boolean requireHumanReview;
}

// TextModerationResponse.java
package com.virtualcompanion.moderationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TextModerationResponse {
    
    private UUID moderationId;
    private String status; // approved, rejected, flagged, review_required
    private Double confidenceScore;
    private List<ViolationDetail> violations;
    private Map<String, Double> categoryScores;
    private String moderatedText; // if content was filtered
    private List<String> flaggedPhrases;
    private String reviewReason;
    private LocalDateTime timestamp;
}

// ImageModerationRequest.java
package com.virtualcompanion.moderationservice.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ImageModerationRequest {
    
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    private UUID characterId;
    
    private String contentType; // avatar, message_attachment, character_image
    
    private Long fileSize;
    
    private String mimeType;
    
    private Map<String, Object> metadata;
}

// ImageModerationResponse.java
package com.virtualcompanion.moderationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageModerationResponse {
    
    private UUID moderationId;
    private String status; // approved, rejected, flagged, review_required
    private Double confidenceScore;
    private List<ViolationDetail> violations;
    private Map<String, DetectionResult> detections;
    private Boolean requiresBlur;
    private List<BoundingBox> sensitiveAreas;
    private LocalDateTime timestamp;
}

// ViolationDetail.java
package com.virtualcompanion.moderationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationDetail {
    
    private String category;
    private String severity; // low, medium, high, critical
    private Double confidence;
    private String description;
    private String action; // block, warn, flag, review
}

// DetectionResult.java
package com.virtualcompanion.moderationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionResult {
    
    private String label;
    private Double confidence;
    private String parentLabel;
    private List<String> taxonomyPath;
}

// BoundingBox.java
package com.virtualcompanion.moderationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBox {
    
    private Double left;
    private Double top;
    private Double width;
    private Double height;
    private String label;
    private Double confidence;
}

// AgeVerificationRequest.java
package com.virtualcompanion.moderationservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeVerificationRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Verification method is required")
    private String verificationMethod; // self_declaration, document, payment
    
    private LocalDate birthDate;
    
    private String documentType;
    
    private String documentImageUrl;
    
    private String documentNumber;
    
    private String jurisdiction;
    
    private Map<String, Object> additionalData;
}

// AgeVerificationResponse.java
package com.virtualcompanion.moderationservice.dto;

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
public class AgeVerificationResponse {
    
    private UUID verificationId;
    private String status; // verified, failed, pending_review, insufficient_data
    private Integer verifiedAge;
    private String verificationMethod;
    private Double confidenceScore;
    private String failureReason;
    private LocalDateTime verifiedAt;
    private LocalDateTime expiresAt;
}

// ContentReportRequest.java
package com.virtualcompanion.moderationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ContentReportRequest {
    
    @NotNull(message = "Reporter ID is required")
    private UUID reporterId;
    
    @NotNull(message = "Content type is required")
    private String contentType; // text, image, video, user, character
    
    @NotNull(message = "Content ID is required")
    private String contentId;
    
    @NotNull(message = "Report reason is required")
    private String reason;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    private List<String> categories;
    
    private Map<String, Object> evidence;
    
    private Boolean isUrgent;
}

// ContentReportResponse.java
package com.virtualcompanion.moderationservice.dto;

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
public class ContentReportResponse {
    
    private UUID reportId;
    private String status; // submitted, under_review, resolved, dismissed
    private String priority; // low, medium, high, critical
    private LocalDateTime submittedAt;
    private LocalDateTime estimatedReviewTime;
    private String referenceNumber;
}

// ModerationDecision.java
package com.virtualcompanion.moderationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationDecision {
    
    private UUID decisionId;
    private UUID moderationRequestId;
    private String decision; // approve, reject, escalate, warn
    private String decisionType; // automated, human, hybrid
    private UUID moderatorId;
    private String reason;
    private List<String> violatedPolicies;
    private Map<String, Object> actions;
    private LocalDateTime decidedAt;
    private Boolean isAppealable;
}

// AppealRequest.java
package com.virtualcompanion.moderationservice.dto;

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
public class AppealRequest {
    
    @NotNull(message = "Decision ID is required")
    private UUID decisionId;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Appeal reason is required")
    @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
    private String reason;
    
    private Map<String, Object> additionalInfo;
}

// AppealResponse.java
package com.virtualcompanion.moderationservice.dto;

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
public class AppealResponse {
    
    private UUID appealId;
    private String status; // submitted, under_review, upheld, overturned
    private LocalDateTime submittedAt;
    private LocalDateTime reviewDeadline;
    private String referenceNumber;
}

// ModerationStatistics.java
package com.virtualcompanion.moderationservice.dto;

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
public class ModerationStatistics {
    
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Long totalContentReviewed;
    private Long automatedDecisions;
    private Long humanDecisions;
    private Map<String, Long> violationsByCategory;
    private Map<String, Long> actionsTaken;
    private Double averageResponseTime;
    private Double automationRate;
    private Long appealsReceived;
    private Long appealsOverturned;
    private Map<String, Object> additionalMetrics;
}