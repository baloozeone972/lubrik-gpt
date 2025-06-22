// ==================== MEDIA SERVICE ====================

// MediaService/src/main/java/com/virtualcompanion/media/controller/MediaController.java
package com.virtualcompanion.media.controller;

import com.virtualcompanion.media.dto.*;
import com.virtualcompanion.media.service.MediaService;
import com.virtualcompanion.media.service.MediaProcessingService;
import com.virtualcompanion.media.service.CDNService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media management endpoints")
public class MediaController {

    private final MediaService mediaService;
    private final MediaProcessingService processingService;
    private final CDNService cdnService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload media file")
    public Mono<ResponseEntity<MediaResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") MediaType type,
            @RequestParam(value = "characterId", required = false) UUID characterId,
            @AuthenticationPrincipal String userId) {

        return mediaService.uploadMedia(file, type, userId, characterId)
                .map(media -> ResponseEntity.status(HttpStatus.CREATED).body(media));
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "Upload multiple media files")
    public Mono<ResponseEntity<List<MediaResponse>>> uploadBatch(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") MediaType type,
            @AuthenticationPrincipal String userId) {

        return mediaService.uploadBatch(files, type, userId)
                .collectList()
                .map(media -> ResponseEntity.status(HttpStatus.CREATED).body(media));
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media by ID")
    public Mono<ResponseEntity<MediaResponse>> getMedia(
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal String userId) {

        return mediaService.getMedia(mediaId, userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user")
    @Operation(summary = "Get user media library")
    public Mono<ResponseEntity<Page<MediaResponse>>> getUserMedia(
            @RequestParam(required = false) MediaType type,
            @RequestParam(required = false) String tag,
            Pageable pageable,
            @AuthenticationPrincipal String userId) {

        return mediaService.getUserMedia(userId, type, tag, pageable)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{mediaId}/process")
    @Operation(summary = "Process media (resize, compress, etc)")
    public Mono<ResponseEntity<MediaProcessingResponse>> processMedia(
            @PathVariable UUID mediaId,
            @RequestBody @Valid MediaProcessingRequest request,
            @AuthenticationPrincipal String userId) {

        return processingService.processMedia(mediaId, request, userId)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media")
    public Mono<ResponseEntity<Void>> deleteMedia(
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal String userId) {

        return mediaService.deleteMedia(mediaId, userId)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @PostMapping("/{mediaId}/cdn/purge")
    @Operation(summary = "Purge media from CDN cache")
    public Mono<ResponseEntity<Void>> purgeCDN(
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal String userId) {

        return cdnService.purgeMedia(mediaId, userId)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get media analytics")
    public Mono<ResponseEntity<MediaAnalyticsResponse>> getAnalytics(
            @RequestParam(required = false) UUID mediaId,
            @RequestParam(required = false) String period,
            @AuthenticationPrincipal String userId) {

        return mediaService.getAnalytics(mediaId, userId, period)
                .map(ResponseEntity::ok);
    }
}

// MediaService/src/main/java/com/virtualcompanion/media/service/MediaService.java
package com.virtualcompanion.media.service;

import com.virtualcompanion.media.dto .*;
        import com.virtualcompanion.media.entity.Media;
import com.virtualcompanion.media.repository.MediaRepository;
import com.virtualcompanion.media.storage.StorageService;
import com.virtualcompanion.media.validator.MediaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final StorageService storageService;
    private final MediaValidator validator;
    private final MediaMapper mapper;

    @Transactional
    public Mono<MediaResponse> uploadMedia(MultipartFile file, MediaType type,
                                           String userId, UUID characterId) {
        return validator.validate(file, type)
                .flatMap(validFile -> storageService.store(validFile, userId))
                .flatMap(storedFile -> {
                    Media media = Media.builder()
                            .id(UUID.randomUUID())
                            .userId(UUID.fromString(userId))
                            .characterId(characterId)
                            .type(type)
                            .fileName(file.getOriginalFilename())
                            .contentType(file.getContentType())
                            .size(file.getSize())
                            .url(storedFile.getUrl())
                            .thumbnailUrl(storedFile.getThumbnailUrl())
                            .metadata(storedFile.getMetadata())
                            .build();

                    return mediaRepository.save(media);
                })
                .map(mapper::toResponse)
                .doOnSuccess(media -> log.info("Media uploaded: {}", media.getId()))
                .doOnError(error -> log.error("Media upload failed", error));
    }

    public Flux<MediaResponse> uploadBatch(List<MultipartFile> files, MediaType type, String userId) {
        return Flux.fromIterable(files)
                .flatMap(file -> uploadMedia(file, type, userId, null))
                .onErrorContinue((error, file) ->
                        log.error("Failed to upload file: {}", ((MultipartFile) file).getOriginalFilename(), error)
                );
    }

    public Mono<MediaResponse> getMedia(UUID mediaId, String userId) {
        return mediaRepository.findByIdAndUserId(mediaId, UUID.fromString(userId))
                .map(mapper::toResponse);
    }

    public Mono<Page<MediaResponse>> getUserMedia(String userId, MediaType type,
                                                  String tag, Pageable pageable) {
        if (type != null && tag != null) {
            return mediaRepository.findByUserIdAndTypeAndTag(UUID.fromString(userId), type, tag, pageable)
                    .map(mapper::toResponse);
        } else if (type != null) {
            return mediaRepository.findByUserIdAndType(UUID.fromString(userId), type, pageable)
                    .map(mapper::toResponse);
        } else {
            return mediaRepository.findByUserId(UUID.fromString(userId), pageable)
                    .map(mapper::toResponse);
        }
    }

    @Transactional
    public Mono<Void> deleteMedia(UUID mediaId, String userId) {
        return mediaRepository.findByIdAndUserId(mediaId, UUID.fromString(userId))
                .flatMap(media ->
                        storageService.delete(media.getUrl())
                                .then(mediaRepository.delete(media))
                )
                .doOnSuccess(v -> log.info("Media deleted: {}", mediaId));
    }

    public Mono<MediaAnalyticsResponse> getAnalytics(UUID mediaId, String userId, String period) {
        return mediaRepository.getAnalytics(mediaId, UUID.fromString(userId), period);
    }
}

// MediaService/src/main/java/com/virtualcompanion/media/service/MediaProcessingService.java
package com.virtualcompanion.media.service;

import com.virtualcompanion.media.dto .*;
        import com.virtualcompanion.media.processor .*;
        import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaProcessingService {

    private final ImageProcessor imageProcessor;
    private final VideoProcessor videoProcessor;
    private final AudioProcessor audioProcessor;
    private final RabbitTemplate rabbitTemplate;

    public Mono<MediaProcessingResponse> processMedia(UUID mediaId,
                                                      MediaProcessingRequest request,
                                                      String userId) {
        MediaProcessingJob job = MediaProcessingJob.builder()
                .jobId(UUID.randomUUID())
                .mediaId(mediaId)
                .userId(userId)
                .operations(request.getOperations())
                .priority(request.getPriority())
                .build();

        // Send to processing queue
        rabbitTemplate.convertAndSend("media.processing", job);

        return Mono.just(MediaProcessingResponse.builder()
                .jobId(job.getJobId())
                .status(ProcessingStatus.QUEUED)
                .estimatedTime(calculateEstimatedTime(request))
                .build());
    }

    private int calculateEstimatedTime(MediaProcessingRequest request) {
        return request.getOperations().stream()
                .mapToInt(op -> switch (op.getType()) {
                    case RESIZE -> 5;
                    case COMPRESS -> 10;
                    case TRANSCODE -> 30;
                    case WATERMARK -> 3;
                    case THUMBNAIL -> 2;
                    default -> 1;
                })
                .sum();
    }
}

// ==================== MODERATION SERVICE ====================

// ModerationService/src/main/java/com/virtualcompanion/moderation/controller/ModerationController.java
package com.virtualcompanion.moderation.controller;

import com.virtualcompanion.moderation.dto .*;
        import com.virtualcompanion.moderation.service.ContentModerationService;
import com.virtualcompanion.moderation.service.UserModerationService;
import com.virtualcompanion.moderation.service.AutoModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation .*;
        import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@Tag(name = "Moderation", description = "Content and user moderation")
public class ModerationController {

    private final ContentModerationService contentService;
    private final UserModerationService userService;
    private final AutoModerationService autoService;

    @PostMapping("/content/check")
    @Operation(summary = "Check content for violations")
    public Mono<ResponseEntity<ModerationResult>> checkContent(
            @RequestBody @Valid ContentCheckRequest request,
            @AuthenticationPrincipal String userId) {

        return contentService.checkContent(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/report")
    @Operation(summary = "Report content or user")
    public Mono<ResponseEntity<ReportResponse>> reportContent(
            @RequestBody @Valid ReportRequest request,
            @AuthenticationPrincipal String userId) {

        return userService.createReport(request, userId)
                .map(response -> ResponseEntity.status(201).body(response));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Get moderation reports")
    public Mono<ResponseEntity<Page<ReportResponse>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportType type,
            Pageable pageable) {

        return userService.getReports(status, type, pageable)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/reports/{reportId}")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Update report status")
    public Mono<ResponseEntity<ReportResponse>> updateReport(
            @PathVariable UUID reportId,
            @RequestBody @Valid ReportUpdateRequest request,
            @AuthenticationPrincipal String moderatorId) {

        return userService.updateReport(reportId, request, moderatorId)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/users/{userId}/ban")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Ban user")
    public Mono<ResponseEntity<BanResponse>> banUser(
            @PathVariable UUID userId,
            @RequestBody @Valid BanRequest request,
            @AuthenticationPrincipal String moderatorId) {

        return userService.banUser(userId, request, moderatorId)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/users/{userId}/ban")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Unban user")
    public Mono<ResponseEntity<Void>> unbanUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal String moderatorId) {

        return userService.unbanUser(userId, moderatorId)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/rules")
    @Operation(summary = "Get moderation rules")
    public Mono<ResponseEntity<List<ModerationRule>>> getRules() {
        return autoService.getRules()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create moderation rule")
    public Mono<ResponseEntity<ModerationRule>> createRule(
            @RequestBody @Valid ModerationRuleRequest request) {

        return autoService.createRule(request)
                .map(rule -> ResponseEntity.status(201).body(rule));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Get moderation statistics")
    public Mono<ResponseEntity<ModerationStats>> getStats(
            @RequestParam(required = false) String period) {

        return autoService.getStats(period)
                .map(ResponseEntity::ok);
    }
}

// ModerationService/src/main/java/com/virtualcompanion/moderation/service/ContentModerationService.java
package com.virtualcompanion.moderation.service;

import com.virtualcompanion.moderation.ai.TextModerationAI;
import com.virtualcompanion.moderation.ai.ImageModerationAI;
import com.virtualcompanion.moderation.dto .*;
        import com.virtualcompanion.moderation.entity.ModerationLog;
import com.virtualcompanion.moderation.repository.ModerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util .*;

@Slf4j
@Service
@RequiredArgsConstructor
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

// ModerationService/src/main/java/com/virtualcompanion/moderation/ai/TextModerationAI.java
package com.virtualcompanion.moderation.ai;

import com.virtualcompanion.moderation.dto.ModerationResult;
import com.virtualcompanion.moderation.dto.ViolationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util .*;

@Slf4j
@Component
@RequiredArgsConstructor
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

// ==================== GATEWAY FILTERS ====================

// Gateway/src/main/java/com/virtualcompanion/gateway/filter/AuthenticationGatewayFilter.java
package com.virtualcompanion.gateway.filter;

import com.virtualcompanion.gateway.security.JwtValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationGatewayFilter extends AbstractGatewayFilterFactory<AuthenticationGatewayFilter.Config> {

    private final JwtValidator jwtValidator;

    public AuthenticationGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            return jwtValidator.validateToken(token)
                    .flatMap(claims -> {
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-User-Id", claims.getSubject())
                                .header("X-User-Roles", String.join(",", claims.getRoles()))
                                .build();

                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    })
                    .onErrorResume(error -> {
                        log.error("JWT validation failed", error);
                        return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"error\":\"%s\",\"status\":%d}", error, httpStatus.value());
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    public static class Config {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

// Gateway/src/main/java/com/virtualcompanion/gateway/filter/RateLimitingGatewayFilter.java
package com.virtualcompanion.gateway.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitingGatewayFilter extends AbstractGatewayFilterFactory<RateLimitingGatewayFilter.Config> {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId == null) {
                userId = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }

            Bucket bucket = buckets.computeIfAbsent(userId, k -> createBucket(config));

            if (bucket.tryConsume(1)) {
                return chain.filter(exchange);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After",
                        String.valueOf(config.getRefillPeriod()));
                return exchange.getResponse().setComplete();
            }
        };
    }

    private Bucket createBucket(Config config) {
        Bandwidth limit = Bandwidth.classic(
                config.getCapacity(),
                Refill.intervally(config.getRefillTokens(), Duration.ofSeconds(config.getRefillPeriod()))
        );
        return Bucket4j.builder().addLimit(limit).build();
    }

    public static class Config {
        private int capacity = 100;
        private int refillTokens = 100;
        private int refillPeriod = 60;

        // Getters and setters
        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(int refillTokens) {
            this.refillTokens = refillTokens;
        }

        public int getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(int refillPeriod) {
            this.refillPeriod = refillPeriod;
        }
    }
}

// Gateway/src/main/java/com/virtualcompanion/gateway/filter/LoggingGatewayFilter.java
package com.virtualcompanion.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
public class LoggingGatewayFilter extends AbstractGatewayFilterFactory<LoggingGatewayFilter.Config> {

    public LoggingGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }

            String requestId = UUID.randomUUID().toString();
            ServerHttpRequest request = exchange.getRequest();

            log.info("Request: {} {} from {} with ID: {}",
                    request.getMethod(),
                    request.getPath(),
                    request.getRemoteAddress(),
                    requestId);

            long startTime = System.currentTimeMillis();

            return chain.filter(exchange)
                    .then(Mono.fromRunnable(() -> {
                        ServerHttpResponse response = exchange.getResponse();
                        long duration = System.currentTimeMillis() - startTime;

                        log.info("Response: {} for request {} in {}ms",
                                response.getStatusCode(),
                                requestId,
                                duration);
                    }));
        };
    }

    public static class Config {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

// Gateway/src/main/java/com/virtualcompanion/gateway/config/GatewayConfiguration.java
package com.virtualcompanion.gateway.config;

import com.virtualcompanion.gateway.filter .*;
        import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfiguration {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                           AuthenticationGatewayFilter authFilter,
                                           RateLimitingGatewayFilter rateLimitFilter,
                                           LoggingGatewayFilter loggingFilter) {
        return builder.routes()
                // User Service
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(200, 60))))
                        .uri("lb://USER-SERVICE"))

                // Character Service
                .route("character-service", r -> r
                        .path("/api/v1/characters/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(100, 60))))
                        .uri("lb://CHARACTER-SERVICE"))

                // Conversation Service
                .route("conversation-service", r -> r
                        .path("/api/v1/conversations/**", "/api/v1/messages/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(300, 60))))
                        .uri("lb://CONVERSATION-SERVICE"))

                // Media Service
                .route("media-service", r -> r
                        .path("/api/v1/media/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(50, 60))))
                        .uri("lb://MEDIA-SERVICE"))

                // Moderation Service
                .route("moderation-service", r -> r
                        .path("/api/v1/moderation/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(100, 60))))
                        .uri("lb://MODERATION-SERVICE"))

                // Billing Service
                .route("billing-service", r -> r
                        .path("/api/v1/billing/**", "/api/v1/subscriptions/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(50, 60))))
                        .uri("lb://BILLING-SERVICE"))

                // WebSocket Route
                .route("websocket", r -> r
                        .path("/ws/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config())))
                        .uri("lb:ws://CONVERSATION-SERVICE"))

                // Public routes (no auth)
                .route("public", r -> r
                        .path("/api/v1/auth/**", "/api/v1/public/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(30, 60))))
                        .uri("lb://USER-SERVICE"))

                .build();
    }

    private RateLimitingGatewayFilter.Config createRateLimitConfig(int capacity, int period) {
        RateLimitingGatewayFilter.Config config = new RateLimitingGatewayFilter.Config();
        config.setCapacity(capacity);
        config.setRefillTokens(capacity);
        config.setRefillPeriod(period);
        return config;
    }
}
