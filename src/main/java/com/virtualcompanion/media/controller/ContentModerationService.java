package com.virtualcompanion.media.controller;

public class ContentModerationService {

    private final TextModerationAI textAI;
    private final ImageModerationAI imageAI;
    private final ModerationLogRepository logRepository;
    private final FilterService filterService;

    public Mono<ModerationResult> checkContent(ContentCheckRequest request) {
        return Mono.defer(() -> {
                    switch (request.getType()) {
                        case TEXT:
                            return checkText(request.getContent());
                        case IMAGE:
                            return checkImage(request.getContent());
                        case MIXED:
                            return checkMixed(request);
                        default:
                            return Mono.just(ModerationResult.safe());
                    }
                })
                .flatMap(result -> logResult(request, result))
                .doOnSuccess(result -> log.debug("Content moderation result: {}", result));
    }

    private Mono<ModerationResult> checkText(String text) {
        // First pass: rule-based filtering
        return filterService.checkText(text)
                .flatMap(ruleResult -> {
                    if (ruleResult.isViolation()) {
                        return Mono.just(ruleResult);
                    }
                    // Second pass: AI moderation
                    return textAI.moderate(text);
                });
    }

    private Mono<ModerationResult> checkImage(String imageUrl) {
        return imageAI.moderate(imageUrl)
                .onErrorReturn(ModerationResult.error("Image moderation failed"));
    }

    private Mono<ModerationResult> checkMixed(ContentCheckRequest request) {
        return Mono.zip(
                        checkText(request.getContent()),
                        checkImage(request.getImageUrl())
                )
                .map(tuple -> ModerationResult.combine(tuple.getT1(), tuple.getT2()));
    }

    private Mono<ModerationResult> logResult(ContentCheckRequest request, ModerationResult result) {
        if (result.isViolation() || result.getConfidence() > 0.7) {
            ModerationLog log = ModerationLog.builder()
                    .id(UUID.randomUUID())
                    .contentType(request.getType())
                    .content(request.getContent())
                    .result(result)
                    .timestamp(new Date())
                    .build();

            return logRepository.save(log)
                    .thenReturn(result);
        }
        return Mono.just(result);
    }
}
