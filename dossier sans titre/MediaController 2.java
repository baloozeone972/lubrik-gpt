package com.virtualcompanion.mediaservice.controller;

public class MediaController {
    
    private final MediaService mediaService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload media file")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MediaUploadResponse> uploadMedia(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute MediaUploadRequest request) {
        
        MediaUploadResponse response = mediaService.uploadMedia(userId, file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media details")
    public ResponseEntity<MediaUploadResponse> getMedia(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID mediaId) {
        
        MediaUploadResponse response = mediaService.getMediaById(userId, mediaId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID mediaId) {
        
        mediaService.deleteMedia(userId, mediaId);
    }
    
    @GetMapping
    @Operation(summary = "Get user's media files")
    public ResponseEntity<Page<MediaUploadResponse>> getUserMedia(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<MediaUploadResponse> media = mediaService.getUserMedia(userId, pageRequest);
        return ResponseEntity.ok(media);
    }
    
    @PostMapping("/search")
    @Operation(summary = "Search media files")
    public ResponseEntity<Page<MediaUploadResponse>> searchMedia(
            @Valid @RequestBody MediaSearchRequest request) {
        
        Page<MediaUploadResponse> results = mediaService.searchMedia(request);
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/{mediaId}/transcode")
    @Operation(summary = "Transcode video file")
    public ResponseEntity<TranscodeResponse> transcodeMedia(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID mediaId,
            @Valid @RequestBody TranscodeRequest request) {
        
        TranscodeResponse response = mediaService.transcodeMedia(userId, mediaId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get user media statistics")
    public ResponseEntity<MediaStatisticsResponse> getStatistics(
            @AuthenticationPrincipal UUID userId) {
        
        MediaStatisticsResponse statistics = mediaService.getUserMediaStatistics(userId);
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/{mediaId}/content")
    @Operation(summary = "Get media file content")
    public ResponseEntity<Resource> getMediaContent(
            @PathVariable UUID mediaId,
            @RequestParam(required = false) String variant) {
        
        byte[] content = mediaService.getMediaContent(mediaId, variant);
        ByteArrayResource resource = new ByteArrayResource(content);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentLength(content.length)
                .body(resource);
    }
    
    @GetMapping("/{mediaId}/thumbnail")
    @Operation(summary = "Get media thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable UUID mediaId) {
        
        byte[] thumbnail = mediaService.getThumbnail(mediaId);
        ByteArrayResource resource = new ByteArrayResource(thumbnail);
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(thumbnail.length)
                .body(resource);
    }
}
