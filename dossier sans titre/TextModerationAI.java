package com.virtualcompanion.moderation.ai;

public class TextModerationAI {

    private final WebClient aiClient;
    private final ToxicityDetector toxicityDetector;

    public Mono<ModerationResult> moderate(String text) {
        return Mono.zip(
                checkToxicity(text),
                checkInappropriateContent(text),
                checkSpam(text)
        )
        .map(tuple -> {
            ModerationResult.Builder builder = ModerationResult.builder();
            
            // Combine all results
            if (tuple.getT1().isViolation()) {
                builder.addViolation(ViolationType.TOXIC, tuple.getT1().getConfidence());
            }
            if (tuple.getT2().isViolation()) {
                builder.addViolation(ViolationType.INAPPROPRIATE, tuple.getT2().getConfidence());
            }
            if (tuple.getT3().isViolation()) {
                builder.addViolation(ViolationType.SPAM, tuple.getT3().getConfidence());
            }
            
            return builder.build();
        })
        .doOnError(error -> log.error("AI moderation failed", error))
        .onErrorReturn(ModerationResult.error("AI moderation unavailable"));
    }

    private Mono<ModerationResult> checkToxicity(String text) {
        return toxicityDetector.detect(text)
                .map(score -> {
                    if (score > 0.8) {
                        return ModerationResult.violation(ViolationType.TOXIC, score);
                    }
                    return ModerationResult.safe();
                });
    }

    private Mono<ModerationResult> checkInappropriateContent(String text) {
        return aiClient.post()
                .uri("/moderate/content")
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    double score = (Double) response.get("inappropriate_score");
                    if (score > 0.7) {
                        return ModerationResult.violation(ViolationType.INAPPROPRIATE, score);
                    }
                    return ModerationResult.safe();
                })
                .onErrorReturn(ModerationResult.safe());
    }

    private Mono<ModerationResult> checkSpam(String text) {
        // Check for spam patterns
        List<String> spamPatterns = Arrays.asList(
                "buy now", "click here", "limited offer", "act now",
                "100% free", "no credit card", "make money fast"
        );
        
        String lowerText = text.toLowerCase();
        long spamCount = spamPatterns.stream()
                .filter(lowerText::contains)
                .count();
        
        if (spamCount >= 3) {
            return Mono.just(ModerationResult.violation(ViolationType.SPAM, 0.9));
        } else if (spamCount >= 1) {
            return Mono.just(ModerationResult.warning(ViolationType.SPAM, 0.5));
        }
        
        return Mono.just(ModerationResult.safe());
    }
}
