// MediaService.java
package com.virtualcompanion.mediaservice.service;

import com.virtualcompanion.mediaservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

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

// MediaServiceImpl.java
package com.virtualcompanion.mediaservice.service.impl;

import com.virtualcompanion.mediaservice.dto.*;
import com.virtualcompanion.mediaservice.entity.MediaFile;
import com.virtualcompanion.mediaservice.entity.MediaVariant;
import com.virtualcompanion.mediaservice.exception.MediaNotFoundException;
import com.virtualcompanion.mediaservice.exception.MediaProcessingException;
import com.virtualcompanion.mediaservice.exception.StorageException;
import com.virtualcompanion.mediaservice.mapper.MediaMapper;
import com.virtualcompanion.mediaservice.repository.MediaFileRepository;
import com.virtualcompanion.mediaservice.repository.MediaFileRepositoryCustom;
import com.virtualcompanion.mediaservice.repository.MediaVariantRepository;
import com.virtualcompanion.mediaservice.service.MediaService;
import com.virtualcompanion.mediaservice.service.StorageService;
import com.virtualcompanion.mediaservice.service.TranscodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
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

// VoiceGenerationService.java
package com.virtualcompanion.mediaservice.service;

import com.virtualcompanion.mediaservice.dto.VoiceGenerationRequest;
import com.virtualcompanion.mediaservice.dto.VoiceGenerationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VoiceGenerationService {
    VoiceGenerationResponse generateVoice(UUID userId, VoiceGenerationRequest request);
    VoiceGenerationResponse getVoiceGeneration(UUID userId, UUID generationId);
    Page<VoiceGenerationResponse> getUserVoiceGenerations(UUID userId, Pageable pageable);
    byte[] getAudioContent(UUID generationId);
    void deleteVoiceGeneration(UUID userId, UUID generationId);
}

// VoiceGenerationServiceImpl.java
package com.virtualcompanion.mediaservice.service.impl;

import com.virtualcompanion.mediaservice.client.CharacterServiceClient;
import com.virtualcompanion.mediaservice.dto.CharacterVoiceConfig;
import com.virtualcompanion.mediaservice.dto.VoiceGenerationRequest;
import com.virtualcompanion.mediaservice.dto.VoiceGenerationResponse;
import com.virtualcompanion.mediaservice.entity.VoiceGeneration;
import com.virtualcompanion.mediaservice.exception.VoiceGenerationException;
import com.virtualcompanion.mediaservice.mapper.VoiceMapper;
import com.virtualcompanion.mediaservice.provider.VoiceProvider;
import com.virtualcompanion.mediaservice.provider.VoiceProviderFactory;
import com.virtualcompanion.mediaservice.repository.VoiceGenerationRepository;
import com.virtualcompanion.mediaservice.service.StorageService;
import com.virtualcompanion.mediaservice.service.VoiceGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VoiceGenerationServiceImpl implements VoiceGenerationService {
    
    private final VoiceGenerationRepository voiceGenerationRepository;
    private final VoiceProviderFactory voiceProviderFactory;
    private final StorageService storageService;
    private final CharacterServiceClient characterServiceClient;
    private final VoiceMapper voiceMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${voice.cache-enabled}")
    private boolean cacheEnabled;
    
    @Value("${voice.default-provider}")
    private String defaultProvider;
    
    @Override
    public VoiceGenerationResponse generateVoice(UUID userId, VoiceGenerationRequest request) {
        log.info("Generating voice for user: {} character: {}", userId, request.getCharacterId());
        
        // Generate text hash for caching
        String textHash = generateTextHash(request.getText());
        
        // Check cache if enabled
        if (cacheEnabled) {
            Optional<VoiceGeneration> cached = voiceGenerationRepository
                    .findCachedGeneration(userId, request.getCharacterId(), textHash);
            
            if (cached.isPresent()) {
                log.info("Returning cached voice generation: {}", cached.get().getId());
                return buildResponse(cached.get());
            }
        }
        
        // Get character voice configuration
        CharacterVoiceConfig voiceConfig = characterServiceClient
                .getCharacterVoiceConfig(request.getCharacterId())
                .orElseThrow(() -> new VoiceGenerationException("Character voice configuration not found"));
        
        // Create voice generation record
        VoiceGeneration generation = VoiceGeneration.builder()
                .userId(userId)
                .characterId(request.getCharacterId())
                .conversationId(request.getConversationId())
                .textContent(request.getText())
                .textHash(textHash)
                .provider(request.getProvider() != null ? request.getProvider() : voiceConfig.getProvider())
                .voiceId(request.getVoiceId() != null ? request.getVoiceId() : voiceConfig.getVoiceId())
                .voiceSettings(convertSettings(request.getSettings()))
                .outputFormat(request.getOutputFormat() != null ? request.getOutputFormat() : "mp3")
                .status("processing")
                .build();
        
        generation = voiceGenerationRepository.save(generation);
        
        // Generate voice asynchronously
        UUID generationId = generation.getId();
        processVoiceGeneration(generationId, request, voiceConfig);
        
        return VoiceGenerationResponse.builder()
                .id(generationId)
                .status("processing")
                .createdAt(generation.getCreatedAt())
                .build();
    }
    
    @Override
    public VoiceGenerationResponse getVoiceGeneration(UUID userId, UUID generationId) {
        VoiceGeneration generation = voiceGenerationRepository.findById(generationId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new VoiceGenerationException("Voice generation not found"));
        
        return buildResponse(generation);
    }
    
    @Override
    public Page<VoiceGenerationResponse> getUserVoiceGenerations(UUID userId, Pageable pageable) {
        return voiceGenerationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::buildResponse);
    }
    
    @Override
    public byte[] getAudioContent(UUID generationId) {
        VoiceGeneration generation = voiceGenerationRepository.findById(generationId)
                .orElseThrow(() -> new VoiceGenerationException("Voice generation not found"));
        
        if (!"completed".equals(generation.getStatus())) {
            throw new VoiceGenerationException("Voice generation not completed");
        }
        
        if (generation.getStoragePath() == null) {
            throw new VoiceGenerationException("Audio file not found");
        }
        
        return storageService.getFileContent(generation.getStoragePath());
    }
    
    @Override
    public void deleteVoiceGeneration(UUID userId, UUID generationId) {
        VoiceGeneration generation = voiceGenerationRepository.findById(generationId)
                .filter(g -> g.getUserId().equals(userId))
                .orElseThrow(() -> new VoiceGenerationException("Voice generation not found"));
        
        // Delete audio file
        if (generation.getStoragePath() != null) {
            try {
                storageService.deleteFile(generation.getStoragePath());
            } catch (Exception e) {
                log.error("Failed to delete audio file: {}", e.getMessage());
            }
        }
        
        // Delete record
        voiceGenerationRepository.delete(generation);
        
        log.info("Voice generation deleted: {}", generationId);
    }
    
    private void processVoiceGeneration(UUID generationId, VoiceGenerationRequest request, CharacterVoiceConfig voiceConfig) {
        // This would typically be done asynchronously
        new Thread(() -> {
            try {
                VoiceGeneration generation = voiceGenerationRepository.findById(generationId).orElseThrow();
                
                // Get appropriate voice provider
                VoiceProvider provider = voiceProviderFactory.getProvider(generation.getProvider());
                
                // Generate audio
                VoiceProvider.VoiceResult result = provider.generateVoice(
                        request.getText(),
                        generation.getVoiceId(),
                        request.getSettings(),
                        generation.getOutputFormat()
                );
                
                // Save audio file
                String storagePath = buildStoragePath(generation);
                storageService.uploadBytes(result.getAudioData(), storagePath);
                
                // Update generation record
                generation.setStoragePath(storagePath);
                generation.setFileSize((long) result.getAudioData().length);
                generation.setDuration(result.getDuration());
                generation.setCost(result.getCost());
                generation.setStatus("completed");
                generation.setCompletedAt(LocalDateTime.now());
                
                voiceGenerationRepository.save(generation);
                
                // Publish event
                publishVoiceGenerationEvent(generation, "completed");
                
                log.info("Voice generation completed: {}", generationId);
                
            } catch (Exception e) {
                log.error("Voice generation failed: {}", e.getMessage());
                
                voiceGenerationRepository.updateStatus(generationId, "failed", e.getMessage());
                
                publishVoiceGenerationEvent(generationId, "failed", e.getMessage());
            }
        }).start();
    }
    
    private String generateTextHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to generate text hash: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }
    
    private Map<String, Object> convertSettings(com.virtualcompanion.mediaservice.dto.VoiceSettings settings) {
        if (settings == null) return null;
        
        Map<String, Object> map = new HashMap<>();
        if (settings.getSpeed() != null) map.put("speed", settings.getSpeed());
        if (settings.getPitch() != null) map.put("pitch", settings.getPitch());
        if (settings.getVolume() != null) map.put("volume", settings.getVolume());
        if (settings.getEmotion() != null) map.put("emotion", settings.getEmotion());
        if (settings.getEmotionIntensity() != null) map.put("emotionIntensity", settings.getEmotionIntensity());
        if (settings.getLanguage() != null) map.put("language", settings.getLanguage());
        if (settings.getStyle() != null) map.put("style", settings.getStyle());
        
        return map;
    }
    
    private String buildStoragePath(VoiceGeneration generation) {
        return String.format("%s/voice/%s/%s.%s",
                generation.getUserId(),
                generation.getCharacterId(),
                generation.getId(),
                generation.getOutputFormat()
        );
    }
    
    private VoiceGenerationResponse buildResponse(VoiceGeneration generation) {
        VoiceGenerationResponse response = voiceMapper.toResponse(generation);
        
        if ("completed".equals(generation.getStatus()) && generation.getStoragePath() != null) {
            response.setAudioUrl(storageService.getSignedUrl(generation.getStoragePath()));
        }
        
        return response;
    }
    
    private void publishVoiceGenerationEvent(VoiceGeneration generation, String status) {
        Map<String, Object> event = Map.of(
                "generationId", generation.getId(),
                "userId", generation.getUserId(),
                "characterId", generation.getCharacterId(),
                "status", status,
                "timestamp", LocalDateTime.now()
        );
        
        kafkaTemplate.send("voice-events", "voice.generation." + status, event);
    }
    
    private void publishVoiceGenerationEvent(UUID generationId, String status, String error) {
        Map<String, Object> event = Map.of(
                "generationId", generationId,
                "status", status,
                "error", error,
                "timestamp", LocalDateTime.now()
        );
        
        kafkaTemplate.send("voice-events", "voice.generation." + status, event);
    }
}