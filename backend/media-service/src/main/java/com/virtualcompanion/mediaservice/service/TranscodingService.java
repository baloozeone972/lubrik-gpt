package com.virtualcompanion.mediaservice.service;

public interface TranscodingService {
    UUID startTranscodingJob(MediaFile mediaFile, TranscodeRequest request);
    void startAsyncTranscoding(UUID mediaFileId, String preset);
    byte[] generateThumbnail(byte[] videoData);
}
