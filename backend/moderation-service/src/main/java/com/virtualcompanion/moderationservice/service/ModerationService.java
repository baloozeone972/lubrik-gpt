package com.virtualcompanion.moderationservice.service;

public interface ModerationService {
    TextModerationResponse moderateText(TextModerationRequest request);

    ImageModerationResponse moderateImage(ImageModerationRequest request);

    ModerationDecision getModerationDecision(UUID moderationId);

    void reviewModeration(UUID moderationId, ModerationDecision decision);

    ContentReportResponse reportContent(ContentReportRequest request);

    AppealResponse submitAppeal(AppealRequest request);

    AgeVerificationResponse verifyAge(AgeVerificationRequest request);

    ModerationStatistics getModerationStatistics(String period);
}
