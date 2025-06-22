package com.virtualcompanion.mediaservice.service;

public interface MediaService {
    MediaUploadResponse uploadMedia(UUID userId, MultipartFile file, MediaUploadRequest request);

    MediaUploadResponse getMediaById(UUID userId, UUID mediaId);

    void deleteMedia(UUID userId, UUID mediaId);

    Page<MediaUploadResponse> getUserMedia(UUID userId, Pageable pageable);

    Page<MediaUploadResponse> searchMedia(MediaSearchRequest request);

    TranscodeResponse transcodeMedia(UUID userId, UUID mediaId, TranscodeRequest request);

    MediaStatisticsResponse getUserMediaStatistics(UUID userId);

    byte[] getMediaContent(UUID mediaId, String variant);

    byte[] getThumbnail(UUID mediaId);
}
