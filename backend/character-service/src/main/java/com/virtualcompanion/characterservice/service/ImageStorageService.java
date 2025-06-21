package com.virtualcompanion.characterservice.service;

public interface ImageStorageService {
    CharacterImage uploadImage(UUID characterId, MultipartFile file);
    void deleteImage(CharacterImage image);
    String generateThumbnail(String originalUrl);
}
