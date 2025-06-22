package com.virtualcompanion.mediaservice.service.impl;

import com.virtualcompanion.mediaservice.repository.MediaFileRepository;
import com.virtualcompanion.mediaservice.repository.MediaVariantRepository;
import com.virtualcompanion.mediaservice.service.MediaService;
import com.virtualcompanion.mediaservice.service.StorageService;
import com.virtualcompanion.mediaservice.service.TranscodingService;

public class MediaServiceImpl implements MediaService {

    private final MediaFileRepository mediaFileRepository;
    private final MediaFileRepositoryCustom mediaFileRepositoryCustom;
    private final MediaVariantRepository variantRepository;
    private final StorageService storageService;
    private final TranscodingService transcodingService;
    private final MediaMapper mediaMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${media.storage.max-file-size}")
    private long maxFileSize;

    @Value("${media.storage.allowed-video-formats}")
    private List<String> allowedVideoFormats;

    @Value("${media.storage.allowed-audio-formats}")
    private List<String> allowedAudioFormats;

    @Value("${media.storage.allowed-image-formats}")
    private List<String> allowedImageFormats;

    @Override
    public MediaUploadResponse uploadMedia(UUID userId, MultipartFile file, MediaUploadRequest request) {
        log.info("Uploading media for user: {} - filename: {}", userId, file.getOriginalFilename());

        // Validate file
        validateFile(file, request.getMediaType());

        try {
            // Generate unique filename
            String fileName = generateFileName(file.getOriginalFilename());
            String storagePath = buildStoragePath(userId, request, fileName);

            // Upload to storage
            String url = storageService.uploadFile(file, storagePath);

            // Create media file entity
            MediaFile mediaFile = MediaFile.builder()
                    .userId(userId)
                    .characterId(request.getCharacterId())
                    .conversationId(request.getConversationId())
                    .fileName(fileName)
                    .originalName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(storagePath)
                    .processingStatus("uploaded")
                    .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                    .metadata(request.getMetadata())
                    .build();

            // Extract media properties
            if (request.getMediaType().equals("video")) {
                Map<String, Object> properties = extractVideoProperties(file);
                mediaFile.setWidth((Integer) properties.get("width"));
                mediaFile.setHeight((Integer) properties.get("height"));
                mediaFile.setDuration((Double) properties.get("duration"));
            } else if (request.getMediaType().equals("audio")) {
                mediaFile.setDuration(extractAudioDuration(file));
            } else if (request.getMediaType().equals("image")) {
                Map<String, Object> properties = extractImageProperties(file);
                mediaFile.setWidth((Integer) properties.get("width"));
                mediaFile.setHeight((Integer) properties.get("height"));
            }

            mediaFile = mediaFileRepository.save(mediaFile);

            // Generate thumbnail for video/image
            if (request.getMediaType().equals("video") || request.getMediaType().equals("image")) {
                generateThumbnail(mediaFile);
            }

            // Publish event
            publishMediaUploadEvent(mediaFile);

            // Start async processing if needed
            if (request.getMediaType().equals("video")) {
                transcodingService.startAsyncTranscoding(mediaFile.getId(), "medium");
            }

            log.info("Media uploaded successfully: {}", mediaFile.getId());

            MediaUploadResponse response = mediaMapper.toUploadResponse(mediaFile);
            response.setUrl(url);
            return response;

        } catch (Exception e) {
            log.error("Failed to upload media: {}", e.getMessage());
            throw new MediaProcessingException("Failed to upload media: " + e.getMessage());
        }
    }

    @Override
    public MediaUploadResponse getMediaById(UUID userId, UUID mediaId) {
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        MediaUploadResponse response = mediaMapper.toUploadResponse(mediaFile);
        response.setUrl(storageService.getSignedUrl(mediaFile.getStoragePath()));

        if (mediaFile.getThumbnailPath() != null) {
            response.setThumbnailUrl(storageService.getSignedUrl(mediaFile.getThumbnailPath()));
        }

        return response;
    }

    @Override
    public void deleteMedia(UUID userId, UUID mediaId) {
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        // Soft delete
        mediaFileRepository.softDelete(mediaId, LocalDateTime.now());

        // Schedule for actual deletion after retention period
        kafkaTemplate.send("media-events", "media.deleted",
                Map.of("mediaId", mediaId, "userId", userId, "scheduledDeletion", LocalDateTime.now().plusDays(30)));

        log.info("Media soft deleted: {}", mediaId);
    }

    @Override
    public Page<MediaUploadResponse> getUserMedia(UUID userId, Pageable pageable) {
        return mediaFileRepository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(mediaFile -> {
                    MediaUploadResponse response = mediaMapper.toUploadResponse(mediaFile);
                    response.setUrl(storageService.getSignedUrl(mediaFile.getStoragePath()));
                    if (mediaFile.getThumbnailPath() != null) {
                        response.setThumbnailUrl(storageService.getSignedUrl(mediaFile.getThumbnailPath()));
                    }
                    return response;
                });
    }

    @Override
    public Page<MediaUploadResponse> searchMedia(MediaSearchRequest request) {
        return mediaFileRepositoryCustom.searchMediaFiles(request)
                .map(mediaFile -> {
                    MediaUploadResponse response = mediaMapper.toUploadResponse(mediaFile);
                    response.setUrl(storageService.getSignedUrl(mediaFile.getStoragePath()));
                    if (mediaFile.getThumbnailPath() != null) {
                        response.setThumbnailUrl(storageService.getSignedUrl(mediaFile.getThumbnailPath()));
                    }
                    return response;
                });
    }

    @Override
    public TranscodeResponse transcodeMedia(UUID userId, UUID mediaId, TranscodeRequest request) {
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        if (!mediaFile.getContentType().startsWith("video/")) {
            throw new MediaProcessingException("Only video files can be transcoded");
        }

        // Start transcoding job
        UUID jobId = transcodingService.startTranscodingJob(mediaFile, request);

        return TranscodeResponse.builder()
                .jobId(jobId)
                .status("queued")
                .progress(0)
                .startedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public MediaStatisticsResponse getUserMediaStatistics(UUID userId) {
        MediaStatisticsResponse stats = new MediaStatisticsResponse();

        // Get total files and size
        stats.setTotalFiles(mediaFileRepository.countByUserIdAndContentType(userId, ""));
        stats.setTotalSize(mediaFileRepository.getTotalStorageSizeByUser(userId));

        // Get files by type
        Map<String, Long> filesByType = new HashMap<>();
        filesByType.put("video", mediaFileRepository.countByUserIdAndContentType(userId, "video"));
        filesByType.put("audio", mediaFileRepository.countByUserIdAndContentType(userId, "audio"));
        filesByType.put("image", mediaFileRepository.countByUserIdAndContentType(userId, "image"));
        stats.setFilesByType(filesByType);

        // Calculate size by type (would need additional queries)
        // This is simplified - in production would have proper aggregation queries
        stats.setSizeByType(filesByType);

        return stats;
    }

    @Override
    public byte[] getMediaContent(UUID mediaId, String variant) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        String path;
        if (variant != null && !variant.equals("original")) {
            MediaVariant mediaVariant = variantRepository
                    .findByMediaFileIdAndVariantTypeAndQuality(mediaId, "transcode", variant)
                    .orElseThrow(() -> new MediaNotFoundException("Variant not found"));
            path = mediaVariant.getStoragePath();
        } else {
            path = mediaFile.getStoragePath();
        }

        return storageService.getFileContent(path);
    }

    @Override
    public byte[] getThumbnail(UUID mediaId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found"));

        if (mediaFile.getThumbnailPath() == null) {
            throw new MediaNotFoundException("Thumbnail not found");
        }

        return storageService.getFileContent(mediaFile.getThumbnailPath());
    }

    private void validateFile(MultipartFile file, String mediaType) {
        if (file.isEmpty()) {
            throw new MediaProcessingException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new MediaProcessingException("File size exceeds maximum allowed size");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new MediaProcessingException("Content type is required");
        }

        List<String> allowedFormats = switch (mediaType) {
            case "video" -> allowedVideoFormats;
            case "audio" -> allowedAudioFormats;
            case "image" -> allowedImageFormats;
            default -> throw new MediaProcessingException("Invalid media type");
        };

        if (!allowedFormats.contains(contentType)) {
            throw new MediaProcessingException("File format not allowed: " + contentType);
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        return UUID.randomUUID().toString() + extension;
    }

    private String buildStoragePath(UUID userId, MediaUploadRequest request, String fileName) {
        StringBuilder path = new StringBuilder();
        path.append(userId.toString()).append("/");

        if (request.getCharacterId() != null) {
            path.append("characters/").append(request.getCharacterId()).append("/");
        }

        if (request.getConversationId() != null) {
            path.append("conversations/").append(request.getConversationId()).append("/");
        }

        path.append(request.getMediaType()).append("/");
        path.append(fileName);

        return path.toString();
    }

    private Map<String, Object> extractVideoProperties(MultipartFile file) {
        // This would use FFprobe or similar to extract video metadata
        // Simplified for example
        Map<String, Object> properties = new HashMap<>();
        properties.put("width", 1920);
        properties.put("height", 1080);
        properties.put("duration", 120.5);
        return properties;
    }

    private Double extractAudioDuration(MultipartFile file) {
        // This would use FFprobe or similar to extract audio duration
        return 180.0; // Simplified
    }

    private Map<String, Object> extractImageProperties(MultipartFile file) {
        // This would use ImageIO or similar to extract image properties
        Map<String, Object> properties = new HashMap<>();
        properties.put("width", 1920);
        properties.put("height", 1080);
        return properties;
    }

    private void generateThumbnail(MediaFile mediaFile) {
        try {
            String thumbnailPath = mediaFile.getStoragePath().replace(".", "_thumb.");
            byte[] thumbnailData = transcodingService.generateThumbnail(
                    storageService.getFileContent(mediaFile.getStoragePath())
            );

            storageService.uploadBytes(thumbnailData, thumbnailPath);

            mediaFile.setThumbnailPath(thumbnailPath);
            mediaFileRepository.save(mediaFile);

        } catch (Exception e) {
            log.error("Failed to generate thumbnail for media {}: {}", mediaFile.getId(), e.getMessage());
        }
    }

    private void publishMediaUploadEvent(MediaFile mediaFile) {
        MediaProcessingEvent event = MediaProcessingEvent.builder()
                .eventType("upload_completed")
                .mediaId(mediaFile.getId())
                .userId(mediaFile.getUserId())
                .processingType("upload")
                .status("completed")
                .timestamp(LocalDateTime.now())
                .data(Map.of("fileSize", mediaFile.getFileSize(), "contentType", mediaFile.getContentType()))
                .build();

        kafkaTemplate.send("media-events", "media.uploaded", event);
    }
}
