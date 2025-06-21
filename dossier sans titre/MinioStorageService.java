package com.virtualcompanion.mediaservice.service.impl;

public class MinioStorageService implements StorageService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.buckets.media}")
    private String mediaBucket;
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;
    
    @Value("${minio.auto-create-bucket}")
    private boolean autoCreateBucket;
    
    public MinioStorageService(@Value("${minio.endpoint}") String endpoint,
                              @Value("${minio.access-key}") String accessKey,
                              @Value("${minio.secret-key}") String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
    
    @PostConstruct
    public void init() {
        if (autoCreateBucket) {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(mediaBucket).build()
                );
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(mediaBucket).build()
                    );
                    log.info("Created bucket: {}", mediaBucket);
                }
            } catch (Exception e) {
                log.error("Error checking/creating bucket: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public String uploadFile(MultipartFile file, String path) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(mediaBucket)
                            .object(path)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            log.info("File uploaded successfully: {}", path);
            return getPublicUrl(path);
            
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage());
            throw new StorageException("Failed to upload file: " + e.getMessage());
        }
    }
    
    @Override
    public String uploadBytes(byte[] data, String path) {
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(mediaBucket)
                            .object(path)
                            .stream(stream, data.length, -1)
                            .build()
            );
            
            log.info("Bytes uploaded successfully: {}", path);
            return getPublicUrl(path);
            
        } catch (Exception e) {
            log.error("Failed to upload bytes: {}", e.getMessage());
            throw new StorageException("Failed to upload bytes: " + e.getMessage());
        }
    }
    
    @Override
    public byte[] getFileContent(String path) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(mediaBucket)
                            .object(path)
                            .build()
            );
            
            return stream.readAllBytes();
            
        } catch (Exception e) {
            log.error("Failed to get file content: {}", e.getMessage());
            throw new StorageException("Failed to get file content: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteFile(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(mediaBucket)
                            .object(path)
                            .build()
            );
            
            log.info("File deleted successfully: {}", path);
            
        } catch (Exception e) {
            log.error("Failed to delete file: {}", e.getMessage());
            throw new StorageException("Failed to delete file: " + e.getMessage());
        }
    }
    
    @Override
    public String getSignedUrl(String path) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(mediaBucket)
                            .object(path)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate signed URL: {}", e.getMessage());
            return getPublicUrl(path);
        }
    }
    
    @Override
    public boolean fileExists(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(mediaBucket)
                            .object(path)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public long getFileSize(String path) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(mediaBucket)
                            .object(path)
                            .build()
            );
            return stat.size();
        } catch (Exception e) {
            log.error("Failed to get file size: {}", e.getMessage());
            return 0;
        }
    }
    
    private String getPublicUrl(String path) {
        return minioEndpoint + "/" + mediaBucket + "/" + path;
    }
}
