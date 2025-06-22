package com.virtualcompanion.characterservice.service;

public class ImageStorageServiceImpl implements ImageStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.characters}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${character.defaults.max-image-size}")
    private long maxImageSize;

    @Value("${character.defaults.allowed-image-types}")
    private List<String> allowedImageTypes;

    @Override
    public CharacterImage uploadImage(UUID characterId, MultipartFile file) {
        // Validate file
        validateFile(file);

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String filename = characterId + "/" + UUID.randomUUID() + extension;

            // Upload original image
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            String imageUrl = minioEndpoint + "/" + bucketName + "/" + filename;

            // Generate and upload thumbnail
            String thumbnailFilename = characterId + "/thumb_" + UUID.randomUUID() + extension;
            byte[] thumbnailData = generateThumbnailBytes(file);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(thumbnailFilename)
                            .stream(new ByteArrayInputStream(thumbnailData), thumbnailData.length, -1)
                            .contentType(file.getContentType())
                            .build()
            );

            String thumbnailUrl = minioEndpoint + "/" + bucketName + "/" + thumbnailFilename;

            // Create image entity
            return CharacterImage.builder()
                    .characterId(characterId)
                    .imageUrl(imageUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .fileName(originalFilename)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .imageType("avatar")
                    .isPrimary(false)
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload image for character: {}", characterId, e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    @Override
    public void deleteImage(CharacterImage image) {
        try {
            // Extract object name from URL
            String objectName = extractObjectName(image.getImageUrl());
            String thumbnailObjectName = extractObjectName(image.getThumbnailUrl());

            // Delete original image
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            // Delete thumbnail
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(thumbnailObjectName)
                            .build()
            );

            log.info("Deleted image and thumbnail for character: {}", image.getCharacterId());

        } catch (Exception e) {
            log.error("Failed to delete image: {}", image.getId(), e);
            throw new RuntimeException("Failed to delete image", e);
        }
    }

    @Override
    public String generateThumbnail(String originalUrl) {
        // This would be used for generating thumbnails for existing images
        // Implementation would download, resize, and re-upload
        return originalUrl; // Simplified for now
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (file.getSize() > maxImageSize) {
            throw new InvalidFileException("File size exceeds maximum allowed size of " + maxImageSize + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedImageTypes.contains(contentType)) {
            throw new InvalidFileException("File type not allowed. Allowed types: " + allowedImageTypes);
        }
    }

    private byte[] generateThumbnailBytes(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(originalImage)
                .size(200, 200)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .outputQuality(0.8)
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }

    private String extractObjectName(String url) {
        // Extract object name from MinIO URL
        String prefix = minioEndpoint + "/" + bucketName + "/";
        return url.substring(prefix.length());
    }
}
