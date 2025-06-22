package com.virtualcompanion.mediaservice.service;

public interface StorageService {
    String uploadFile(MultipartFile file, String path);

    String uploadBytes(byte[] data, String path);

    byte[] getFileContent(String path);

    void deleteFile(String path);

    String getSignedUrl(String path);

    boolean fileExists(String path);

    long getFileSize(String path);
}
