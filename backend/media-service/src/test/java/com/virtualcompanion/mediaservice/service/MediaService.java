package com.virtualcompanion.media.service;

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
