package com.virtualcompanion.media.controller;

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
