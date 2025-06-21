package com.virtualcompanion.mediaservice.repository;

public interface MediaFileRepositoryCustom {
    Page<MediaFile> searchMediaFiles(MediaSearchRequest request);
}
