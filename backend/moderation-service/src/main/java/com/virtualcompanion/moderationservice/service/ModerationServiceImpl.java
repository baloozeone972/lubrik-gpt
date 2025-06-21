package com.virtualcompanion.moderationservice.service;

public class ModerationServiceImpl implements ModerationService {
    
    private final ModerationRequestRepository requestRepository;
    private final ModerationResultRepository resultRepository;
    private final BlockedContentRepository blockedContentRepository;
    private final UserModerationHistoryRepository historyRepository;
    private final ContentReportRepository reportRepository;
    private final AppealRepository appealRepository;
    private final AgeVerificationRepository ageVerificationRepository;
    
    private final ModerationProviderFactory providerFactory;
    private final RuleEngineService ruleEngine;
    private final UserServiceClient userServiceClient;
    private final ModerationMapper mapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${moderation.queue.auto-moderation.confidence-threshold}")
    private double autoModerationThreshold;
    
    @Value("${moderation.queue.human-review.escalation-threshold}")
    private double humanReviewThreshold;
    
    @Override
    public TextModerationResponse moderateText(TextModerationRequest request) {
        log.info("Moderating text for user: {} content type: {}", request.getUserId(), request.getContentType());
        
        try {
            // Create moderation request record
            ModerationRequest moderationRequest = ModerationRequest.builder()
                    .contentType("text")
                    .contentId(generateContentId(request.getText()))
                    .userId(request.getUserId())
                    .characterId(request.getCharacterId())
                    .conversationId(request.getConversationId())
                    .status("pending")
                    .priority(determinePriority(request))
                    .requiresHumanReview(request.getRequireHumanReview())
                    .build();
            
            moderationRequest = requestRepository.save(moderationRequest);
            
            // Check if content is already blocked
            String contentHash = generateHash(request.getText());
            if (blockedContentRepository.existsByContentHash(contentHash)) {
                return createBlockedResponse(moderationRequest.getId(), "Content is blocked");
            }
            
            // Get user's jurisdiction and age
            UserDetails userDetails = userServiceClient.getUser(request.getUserId());
            String jurisdiction = userDetails.getJurisdiction();
            Integer userAge = userDetails.getVerifiedAge();
            
            // Apply jurisdiction-specific rules
            List<ModerationRule> rules = ruleEngine.getApplicableRules("text", jurisdiction, userAge);
            
            // Run through moderation providers
            List<ModerationProvider> providers = providerFactory.getProvidersForType("text");
            Map<String, ModerationProvider.Result> providerResults = new HashMap<>();
            
            for (ModerationProvider provider : providers) {
                try {
                    ModerationProvider.Result result = provider.moderateText(request.getText(), request.getLanguage());
                    providerResults.put(provider.getName(), result);
                } catch (Exception e) {
                    log.error("Provider {} failed: {}", provider.getName(), e.getMessage());
                }
            }
            
            // Aggregate results
            AggregatedModerationResult aggregated = aggregateResults(providerResults, rules);
            
            // Make decision
            String status;
            boolean requiresReview = false;
            
            if (aggregated.getConfidenceScore() >= autoModerationThreshold && !aggregated.hasViolations()) {
                status = "approved";
            } else if (aggregated.hasViolations() && aggregated.getConfidenceScore() >= autoModerationThreshold) {
                status = "rejected";
            } else if (aggregated.getConfidenceScore() >= humanReviewThreshold || request.getRequireHumanReview()) {
                status = "flagged";
                requiresReview = true;
            } else {
                status = "approved"; // Low confidence, no clear violations
            }
            
            // Update moderation request
            moderationRequest.setStatus(status);
            moderationRequest.setProcessedAt(LocalDateTime.now());
            moderationRequest.setConfidenceScore(aggregated.getConfidenceScore());
            moderationRequest.setViolationCategory(aggregated.getPrimaryViolation());
            moderationRequest.setRequiresHumanReview(requiresReview);
            requestRepository.save(moderationRequest);
            
            // Create moderation result
            ModerationResult result = ModerationResult.builder()
                    .moderationRequestId(moderationRequest.getId())
                    .decision(status.equals("approved") ? "approve" : status.equals("rejected") ? "reject" : "escalate")
                    .decisionType("automated")
                    .reason(aggregated.getReason())
                    .violatedPolicies(aggregated.getViolatedPolicies())
                    .confidenceScore(aggregated.getConfidenceScore())
                    .providerResults(providerResults)
                    .build();
            
            resultRepository.save(result);
            
            // Record in user history if violation
            if ("rejected".equals(status)) {
                recordUserViolation(request.getUserId(), "text_violation", aggregated.getPrimaryViolation());
            }
            
            // Publish event
            publishModerationEvent(moderationRequest, result);
            
            // Build response
            return TextModerationResponse.builder()
                    .moderationId(moderationRequest.getId())
                    .status(status)
                    .confidenceScore(aggregated.getConfidenceScore())
                    .violations(aggregated.getViolations())
                    .categoryScores(aggregated.getCategoryScores())
                    .moderatedText(aggregated.getModeratedText())
                    .flaggedPhrases(aggregated.getFlaggedPhrases())
                    .reviewReason(requiresReview ? aggregated.getReason() : null)
                    .timestamp(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Text moderation failed: {}", e.getMessage());
            throw new ModerationException("Text moderation failed: " + e.getMessage());
        }
    }
    
    @Override
    public ImageModerationResponse moderateImage(ImageModerationRequest request) {
        log.info("Moderating image for user: {} content type: {}", request.getUserId(), request.getContentType());
        
        try {
            // Create moderation request record
            ModerationRequest moderationRequest = ModerationRequest.builder()
                    .contentType("image")
                    .contentId(request.getImageUrl())
                    .userId(request.getUserId())
                    .characterId(request.getCharacterId())
                    .status("pending")
                    .priority(determinePriority(request))
                    .metadata(request.getMetadata())
                    .build();
            
            moderationRequest = requestRepository.save(moderationRequest);
            
            // Get user details
            UserDetails userDetails = userServiceClient.getUser(request.getUserId());
            String jurisdiction = userDetails.getJurisdiction();
            Integer userAge = userDetails.getVerifiedAge();
            
            // Apply rules
            List<ModerationRule> rules = ruleEngine.getApplicableRules("image", jurisdiction, userAge);
            
            // Run through moderation providers
            List<ModerationProvider> providers = providerFactory.getProvidersForType("image");
            Map<String, ModerationProvider.ImageResult> providerResults = new HashMap<>();
            
            for (ModerationProvider provider : providers) {
                try {
                    ModerationProvider.ImageResult result = provider.moderateImage(request.getImageUrl());
                    providerResults.put(provider.getName(), result);
                } catch (Exception e) {
                    log.error("Provider {} failed: {}", provider.getName(), e.getMessage());
                }
            }
            
            // Aggregate results
            AggregatedImageResult aggregated = aggregateImageResults(providerResults, rules);
            
            // Make decision
            String status;
            boolean requiresReview = false;
            
            if (aggregated.getConfidenceScore() >= autoModerationThreshold && !aggregated.hasViolations()) {
                status = "approved";
            } else if (aggregated.hasViolations() && aggregated.getConfidenceScore() >= autoModerationThreshold) {
                status = "rejected";
            } else {
                status = "flagged";
                requiresReview = true;
            }
            
            // Update moderation request
            moderationRequest.setStatus(status);
            moderationRequest.setProcessedAt(LocalDateTime.now());
            moderationRequest.setConfidenceScore(aggregated.getConfidenceScore());
            moderationRequest.setViolationCategory(aggregated.getPrimaryViolation());
            moderationRequest.setRequiresHumanReview(requiresReview);
            requestRepository.save(moderationRequest);
            
            // Create moderation result
            ModerationResult result = ModerationResult.builder()
                    .moderationRequestId(moderationRequest.getId())
                    .decision(status.equals("approved") ? "approve" : status.equals("rejected") ? "reject" : "escalate")
                    .decisionType("automated")
                    .reason(aggregated.getReason())
                    .violatedPolicies(aggregated.getViolatedPolicies())
                    .confidenceScore(aggregated.getConfidenceScore())
                    .build();
            
            resultRepository.save(result);
            
            // Record violation if needed
            if ("rejected".equals(status)) {
                recordUserViolation(request.getUserId(), "image_violation", aggregated.getPrimaryViolation());
                
                // Block image hash if severe violation
                if (aggregated.getSeverity().equals("critical")) {
                    blockContent(request.getImageUrl(), "image", aggregated.getReason());
                }
            }
            
            // Publish event
            publishModerationEvent(moderationRequest, result);
            
            // Build response
            return ImageModerationResponse.builder()
                    .moderationId(moderationRequest.getId())
                    .status(status)
                    .confidenceScore(aggregated.getConfidenceScore())
                    .violations(aggregated.getViolations())
                    .detections(aggregated.getDetections())
                    .requiresBlur(aggregated.isRequiresBlur())
                    .sensitiveAreas(aggregated.getSensitiveAreas())
                    .timestamp(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Image moderation failed: {}", e.getMessage());
            throw new ModerationException("Image moderation failed: " + e.getMessage());
        }
    }
    
    @Override
    public ModerationDecision getModerationDecision(UUID moderationId) {
        ModerationResult result = resultRepository.findByModerationRequestId(moderationId)
                .orElseThrow(() -> new ModerationException("Moderation decision not found"));
        
        return mapper.toDecision(result);
    }
    
    @Override
    public void reviewModeration(UUID moderationId, ModerationDecision decision) {
        ModerationRequest request = requestRepository.findById(moderationId)
                .orElseThrow(() -> new ModerationException("Moderation request not found"));
        
        // Update request status
        request.setStatus(decision.getDecision().equals("approve") ? "approved" : "rejected");
        request.setProcessedAt(LocalDateTime.now());
        request.setRequiresHumanReview(false);
        requestRepository.save(request);
        
        // Create or update result
        ModerationResult result = resultRepository.findByModerationRequestId(moderationId)
                .orElse(ModerationResult.builder()
                        .moderationRequestId(moderationId)
                        .build());
        
        result.setDecision(decision.getDecision());
        result.setDecisionType("human");
        result.setModeratorId(decision.getModeratorId());
        result.setReason(decision.getReason());
        result.setViolatedPolicies(decision.getViolatedPolicies());
        result.setCreatedAt(LocalDateTime.now());
        
        resultRepository.save(result);
        
        // Take actions based on decision
        if ("reject".equals(decision.getDecision())) {
            handleRejection(request, decision);
        }
        
        // Publish event
        publishModerationEvent(request, result);
    }
    
    @Override
    public ContentReportResponse reportContent(ContentReportRequest request) {
        log.info("Content report from user: {} for content: {}", request.getReporterId(), request.getContentId());
        
        // Create report
        ContentReport report = ContentReport.builder()
                .reporterId(request.getReporterId())
                .contentType(request.getContentType())
                .contentId(request.getContentId())
                .reason(request.getReason())
                .description(request.getDescription())
                .categories(request.getCategories())
                .evidence(request.getEvidence())
                .status("submitted")
                .priority(determinePriority(request))
                .submittedAt(LocalDateTime.now())
                .referenceNumber(generateReferenceNumber())
                .build();
        
        report = reportRepository.save(report);
        
        // Check if content needs immediate action
        if (Boolean.TRUE.equals(request.getIsUrgent()) || isHighPriorityCategory(request.getCategories())) {
            escalateReport(report);
        }
        
        // Trigger moderation if multiple reports
        long reportCount = reportRepository.findByContent(request.getContentId(), request.getContentType()).size();
        if (reportCount >= 3) {
            triggerContentModeration(request.getContentId(), request.getContentType());
        }
        
        // Publish event
        kafkaTemplate.send("moderation-events", "content.reported", 
            Map.of("reportId", report.getId(), "contentId", request.getContentId()));
        
        return ContentReportResponse.builder()
                .reportId(report.getId())
                .status(report.getStatus())
                .priority(report.getPriority())
                .submittedAt(report.getSubmittedAt())
                .estimatedReviewTime(calculateReviewTime(report.getPriority()))
                .referenceNumber(report.getReferenceNumber())
                .build();
    }
    
    @Override
    public AppealResponse submitAppeal(AppealRequest request) {
        log.info("Appeal submitted by user: {} for decision: {}", request.getUserId(), request.getDecisionId());
        
        // Verify decision exists and belongs to user
        ModerationResult result = resultRepository.findById(request.getDecisionId())
                .orElseThrow(() -> new ModerationException("Decision not found"));
        
        // Check if already appealed
        if (appealRepository.findByDecisionIdAndUserId(request.getDecisionId(), request.getUserId()).isPresent()) {
            throw new ModerationException("Appeal already submitted for this decision");
        }
        
        // Create appeal
        Appeal appeal = Appeal.builder()
                .decisionId(request.getDecisionId())
                .userId(request.getUserId())
                .reason(request.getReason())
                .additionalInfo(request.getAdditionalInfo())
                .status("submitted")
                .submittedAt(LocalDateTime.now())
                .reviewDeadline(LocalDateTime.now().plusDays(3))
                .referenceNumber(generateReferenceNumber())
                .build();
        
        appeal = appealRepository.save(appeal);
        
        // Publish event
        kafkaTemplate.send("moderation-events", "appeal.submitted", 
            Map.of("appealId", appeal.getId(), "userId", request.getUserId()));
        
        return AppealResponse.builder()
                .appealId(appeal.getId())
                .status(appeal.getStatus())
                .submittedAt(appeal.getSubmittedAt())
                .reviewDeadline(appeal.getReviewDeadline())
                .referenceNumber(appeal.getReferenceNumber())
                .build();
    }
    
    @Override
    public AgeVerificationResponse verifyAge(AgeVerificationRequest request) {
        log.info("Age verification for user: {} method: {}", request.getUserId(), request.getVerificationMethod());
        
        try {
            // Check if user already has valid verification
            Optional<AgeVerification> existing = ageVerificationRepository
                    .findValidVerification(request.getUserId(), LocalDateTime.now());
            
            if (existing.isPresent()) {
                return mapper.toAgeVerificationResponse(existing.get());
            }
            
            // Create new verification
            AgeVerification verification = AgeVerification.builder()
                    .userId(request.getUserId())
                    .verificationMethod(request.getVerificationMethod())
                    .status("pending")
                    .jurisdiction(request.getJurisdiction())
                    .build();
            
            verification = ageVerificationRepository.save(verification);
            
            // Process based on method
            AgeVerificationResult result = switch (request.getVerificationMethod()) {
                case "self_declaration" -> processSelfDeclaration(request);
                case "document" -> processDocumentVerification(request);
                case "payment" -> processPaymentVerification(request);
                default -> throw new ModerationException("Invalid verification method");
            };
            
            // Update verification
            verification.setStatus(result.isVerified() ? "verified" : "failed");
            verification.setVerifiedAge(result.getAge());
            verification.setConfidenceScore(result.getConfidenceScore());
            verification.setFailureReason(result.getFailureReason());
            verification.setVerifiedAt(LocalDateTime.now());
            verification.setExpiresAt(result.isVerified() ? LocalDateTime.now().plusYears(1) : null);
            
            verification = ageVerificationRepository.save(verification);
            
            // Update user service
            if (result.isVerified()) {
                userServiceClient.updateVerifiedAge(request.getUserId(), result.getAge());
            }
            
            // Publish event
            kafkaTemplate.send("moderation-events", "age.verification." + verification.getStatus(), 
                Map.of("userId", request.getUserId(), "age", result.getAge()));
            
            return mapper.toAgeVerificationResponse(verification);
            
        } catch (Exception e) {
            log.error("Age verification failed: {}", e.getMessage());
            throw new ModerationException("Age verification failed: " + e.getMessage());
        }
    }
    
    @Override
    public ModerationStatistics getModerationStatistics(String period) {
        LocalDateTime startDate = calculateStartDate(period);
        LocalDateTime endDate = LocalDateTime.now();
        
        // Gather statistics
        Long totalReviewed = requestRepository.count();
        Long automatedDecisions = resultRepository.countAutomatedDecisions(startDate);
        Long humanDecisions = totalReviewed - automatedDecisions;
        
        // Get violations by category
        List<Object[]> violationStats = requestRepository.getViolationStatistics(startDate);
        Map<String, Long> violationsByCategory = violationStats.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
        
        // Get average response time
        Double avgResponseTime = resultRepository.getAverageResponseTime(startDate);
        
        // Get appeals data
        Long appealsReceived = appealRepository.count();
        Long appealsOverturned = appealRepository.countOverturnedAppealsSince(startDate);
        
        return ModerationStatistics.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalContentReviewed(totalReviewed)
                .automatedDecisions(automatedDecisions)
                .humanDecisions(humanDecisions)
                .violationsByCategory(violationsByCategory)
                .averageResponseTime(avgResponseTime)
                .automationRate(totalReviewed > 0 ? (double) automatedDecisions / totalReviewed : 0)
                .appealsReceived(appealsReceived)
                .appealsOverturned(appealsOverturned)
                .build();
    }
    
    // Helper methods
    
    private String generateContentId(String content) {
        return UUID.randomUUID().toString();
    }
    
    private String generateHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
    
    private String determinePriority(Object request) {
        // Logic to determine priority based on content type and context
        return "medium";
    }
    
    private TextModerationResponse createBlockedResponse(UUID moderationId, String reason) {
        return TextModerationResponse.builder()
                .moderationId(moderationId)
                .status("rejected")
                .confidenceScore(1.0)
                .violations(List.of(ViolationDetail.builder()
                        .category("blocked_content")
                        .severity("critical")
                        .confidence(1.0)
                        .description(reason)
                        .action("block")
                        .build()))
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    private AggregatedModerationResult aggregateResults(Map<String, ModerationProvider.Result> results, 
                                                       List<ModerationRule> rules) {
        // Aggregate logic for text moderation results
        // This would combine results from multiple providers and apply rules
        return new AggregatedModerationResult();
    }
    
    private AggregatedImageResult aggregateImageResults(Map<String, ModerationProvider.ImageResult> results,
                                                       List<ModerationRule> rules) {
        // Aggregate logic for image moderation results
        return new AggregatedImageResult();
    }
    
    private void recordUserViolation(UUID userId, String action, String category) {
        UserModerationHistory history = UserModerationHistory.builder()
                .userId(userId)
                .action(action)
                .category(category)
                .severity(determineSeverity(category))
                .createdAt(LocalDateTime.now())
                .build();
        
        historyRepository.save(history);
    }
    
    private void blockContent(String content, String contentType, String reason) {
        String hash = generateHash(content);
        
        BlockedContent blocked = BlockedContent.builder()
                .contentHash(hash)
                .contentType(contentType)
                .reason(reason)
                .blockedAt(LocalDateTime.now())
                .build();
        
        blockedContentRepository.save(blocked);
    }
    
    private void publishModerationEvent(ModerationRequest request, ModerationResult result) {
        Map<String, Object> event = Map.of(
                "moderationId", request.getId(),
                "userId", request.getUserId(),
                "contentType", request.getContentType(),
                "status", request.getStatus(),
                "decision", result.getDecision(),
                "timestamp", LocalDateTime.now()
        );
        
        kafkaTemplate.send("moderation-events", "moderation.completed", event);
    }
    
    private void handleRejection(ModerationRequest request, ModerationDecision decision) {
        // Take appropriate action based on rejection
        // This could include blocking content, warning user, etc.
    }
    
    private boolean isHighPriorityCategory(List<String> categories) {
        List<String> highPriority = List.of("child-safety", "self-harm", "terrorism");
        return categories != null && categories.stream().anyMatch(highPriority::contains);
    }
    
    private void escalateReport(ContentReport report) {
        report.setPriority("high");
        reportRepository.save(report);
        
        // Notify moderators
        kafkaTemplate.send("moderation-events", "report.escalated", 
            Map.of("reportId", report.getId(), "priority", "high"));
    }
    
    private void triggerContentModeration(String contentId, String contentType) {
        // Trigger automatic moderation of reported content
        kafkaTemplate.send("moderation-events", "content.review.triggered", 
            Map.of("contentId", contentId, "contentType", contentType));
    }
    
    private LocalDateTime calculateReviewTime(String priority) {
        return switch (priority) {
            case "high" -> LocalDateTime.now().plusHours(1);
            case "medium" -> LocalDateTime.now().plusHours(4);
            default -> LocalDateTime.now().plusDays(1);
        };
    }
    
    private String generateReferenceNumber() {
        return "REF-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private AgeVerificationResult processSelfDeclaration(AgeVerificationRequest request) {
        // Process self-declaration
        if (request.getBirthDate() != null) {
            int age = LocalDateTime.now().getYear() - request.getBirthDate().getYear();
            return new AgeVerificationResult(true, age, 0.7, null);
        }
        return new AgeVerificationResult(false, null, 0.0, "Birth date required");
    }
    
    private AgeVerificationResult processDocumentVerification(AgeVerificationRequest request) {
        // This would use AI providers to verify documents
        return new AgeVerificationResult(true, 25, 0.95, null);
    }
    
    private AgeVerificationResult processPaymentVerification(AgeVerificationRequest request) {
        // This would verify through payment method
        return new AgeVerificationResult(true, 18, 0.9, null);
    }
    
    private String determineSeverity(String category) {
        // Determine severity based on category
        return "medium";
    }
    
    private LocalDateTime calculateStartDate(String period) {
        return switch (period.toLowerCase()) {
            case "daily" -> LocalDateTime.now().minusDays(1);
            case "weekly" -> LocalDateTime.now().minusWeeks(1);
            case "monthly" -> LocalDateTime.now().minusMonths(1);
            case "yearly" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.now().minusDays(30);
        };
    }
    
    // Inner classes for aggregation results
    
    @lombok.Data
    @lombok.Builder
    private static class AggregatedModerationResult {
        private double confidenceScore;
        private boolean hasViolations;
        private String primaryViolation;
        private String reason;
        private List<String> violatedPolicies;
        private List<ViolationDetail> violations;
        private Map<String, Double> categoryScores;
        private String moderatedText;
        private List<String> flaggedPhrases;
    }
    
    @lombok.Data
    @lombok.Builder
    private static class AggregatedImageResult {
        private double confidenceScore;
        private boolean hasViolations;
        private String primaryViolation;
        private String reason;
        private String severity;
        private List<String> violatedPolicies;
        private List<ViolationDetail> violations;
        private Map<String, DetectionResult> detections;
        private boolean requiresBlur;
        private List<BoundingBox> sensitiveAreas;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AgeVerificationResult {
        private boolean verified;
        private Integer age;
        private Double confidenceScore;
        private String failureReason;
    }
}
