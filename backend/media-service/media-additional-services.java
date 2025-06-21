// StorageService.java
package com.virtualcompanion.mediaservice.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadFile(MultipartFile file, String path);
    String uploadBytes(byte[] data, String path);
    byte[] getFileContent(String path);
    void deleteFile(String path);
    String getSignedUrl(String path);
    boolean fileExists(String path);
    long getFileSize(String path);
}

// MinioStorageService.java
package com.virtualcompanion.mediaservice.service.impl;

import com.virtualcompanion.mediaservice.exception.StorageException;
import com.virtualcompanion.mediaservice.service.StorageService;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
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

// StreamingService.java
package com.virtualcompanion.mediaservice.service;

import com.virtualcompanion.mediaservice.dto.*;

import java.util.UUID;

public interface StreamingService {
    StreamingResponse startSession(UUID userId, StreamingRequest request);
    void setSdpAnswer(UUID userId, String sessionId, String sdpAnswer);
    void addIceCandidate(UUID userId, String sessionId, IceCandidate candidate);
    void endSession(UUID userId, String sessionId);
    StreamingSessionResponse getSession(UUID userId, String sessionId);
}

// KurentoStreamingService.java
package com.virtualcompanion.mediaservice.service.impl;

import com.virtualcompanion.mediaservice.dto.*;
import com.virtualcompanion.mediaservice.entity.StreamingSession;
import com.virtualcompanion.mediaservice.exception.StreamingException;
import com.virtualcompanion.mediaservice.mapper.StreamingMapper;
import com.virtualcompanion.mediaservice.repository.StreamingSessionRepository;
import com.virtualcompanion.mediaservice.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class KurentoStreamingService implements StreamingService {
    
    private final KurentoClient kurentoClient;
    private final StreamingSessionRepository sessionRepository;
    private final StreamingMapper streamingMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private final Map<String, MediaPipeline> pipelines = new ConcurrentHashMap<>();
    private final Map<String, WebRtcEndpoint> endpoints = new ConcurrentHashMap<>();
    
    @Value("${webrtc.max-bandwidth.video}")
    private int maxVideoBandwidth;
    
    @Value("${webrtc.max-bandwidth.audio}")
    private int maxAudioBandwidth;
    
    @Override
    public StreamingResponse startSession(UUID userId, StreamingRequest request) {
        log.info("Starting streaming session for user: {} character: {}", userId, request.getCharacterId());
        
        try {
            // Check concurrent sessions limit
            long activeSessions = sessionRepository.countActiveSessionsByUser(userId);
            if (activeSessions >= 3) {
                throw new StreamingException("Maximum concurrent sessions reached");
            }
            
            // Create Kurento media pipeline
            MediaPipeline pipeline = kurentoClient.createMediaPipeline();
            
            // Create WebRTC endpoint
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            
            // Configure bandwidth limits
            webRtcEndpoint.setMaxVideoSendBandwidth(maxVideoBandwidth);
            webRtcEndpoint.setMaxAudioSendBandwidth(maxAudioBandwidth);
            
            // Add ICE candidate listener
            List<IceCandidate> candidates = new ArrayList<>();
            webRtcEndpoint.addIceCandidateFoundListener(event -> {
                IceCandidate candidate = streamingMapper.toDto(event.getCandidate());
                candidates.add(candidate);
                
                // Send candidate to client via WebSocket
                publishIceCandidate(userId.toString(), candidate);
            });
            
            // Configure recording if enabled
            if (request.getConfig() != null && Boolean.TRUE.equals(request.getConfig().getEnableRecording())) {
                configureRecording(pipeline, webRtcEndpoint, userId, request);
            }
            
            // Generate offer
            String sdpOffer = webRtcEndpoint.generateOffer();
            
            // Create session
            String sessionId = UUID.randomUUID().toString();
            StreamingSession session = StreamingSession.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .characterId(request.getCharacterId())
                    .conversationId(request.getConversationId())
                    .sessionType(request.getStreamType())
                    .kurentoSessionId(pipeline.getId())
                    .sdpOffer(sdpOffer)
                    .iceCandiates(JsonUtils.toJson(candidates))
                    .status("waiting_answer")
                    .metadata(request.getMetadata())
                    .build();
            
            session = sessionRepository.save(session);
            
            // Store references
            pipelines.put(sessionId, pipeline);
            endpoints.put(sessionId, webRtcEndpoint);
            
            // Gather ICE candidates
            webRtcEndpoint.gatherCandidates();
            
            // Publish event
            publishStreamingEvent(session, "session_started");
            
            return StreamingResponse.builder()
                    .sessionId(sessionId)
                    .kurentoSessionId(pipeline.getId())
                    .sdpOffer(sdpOffer)
                    .iceCandidates(candidates)
                    .status("waiting_answer")
                    .startedAt(session.getStartedAt())
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to start streaming session: {}", e.getMessage());
            throw new StreamingException("Failed to start streaming session: " + e.getMessage());
        }
    }
    
    @Override
    public void setSdpAnswer(UUID userId, String sessionId, String sdpAnswer) {
        StreamingSession session = sessionRepository.findBySessionId(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new StreamingException("Session not found"));
        
        WebRtcEndpoint endpoint = endpoints.get(sessionId);
        if (endpoint == null) {
            throw new StreamingException("WebRTC endpoint not found");
        }
        
        try {
            // Process SDP answer
            endpoint.processAnswer(sdpAnswer);
            
            // Update session
            sessionRepository.updateSdpAnswer(sessionId, sdpAnswer);
            session.setStatus("connected");
            sessionRepository.save(session);
            
            // Publish event
            publishStreamingEvent(session, "session_connected");
            
            log.info("SDP answer processed for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to process SDP answer: {}", e.getMessage());
            throw new StreamingException("Failed to process SDP answer: " + e.getMessage());
        }
    }
    
    @Override
    public void addIceCandidate(UUID userId, String sessionId, IceCandidate candidate) {
        StreamingSession session = sessionRepository.findBySessionId(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new StreamingException("Session not found"));
        
        WebRtcEndpoint endpoint = endpoints.get(sessionId);
        if (endpoint == null) {
            throw new StreamingException("WebRTC endpoint not found");
        }
        
        try {
            // Add ICE candidate
            org.kurento.client.IceCandidate kurentoCandidate = new org.kurento.client.IceCandidate(
                    candidate.getCandidate(),
                    candidate.getSdpMid(),
                    Integer.parseInt(candidate.getSdpMLineIndex())
            );
            
            endpoint.addIceCandidate(kurentoCandidate);
            
            log.debug("ICE candidate added for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to add ICE candidate: {}", e.getMessage());
        }
    }
    
    @Override
    public void endSession(UUID userId, String sessionId) {
        StreamingSession session = sessionRepository.findBySessionId(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new StreamingException("Session not found"));
        
        try {
            // Clean up Kurento resources
            WebRtcEndpoint endpoint = endpoints.remove(sessionId);
            if (endpoint != null) {
                endpoint.release();
            }
            
            MediaPipeline pipeline = pipelines.remove(sessionId);
            if (pipeline != null) {
                pipeline.release();
            }
            
            // Update session
            sessionRepository.endSession(sessionId, "ended", LocalDateTime.now());
            
            // Publish event
            publishStreamingEvent(session, "session_ended");
            
            log.info("Streaming session ended: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to end session: {}", e.getMessage());
        }
    }
    
    @Override
    public StreamingSessionResponse getSession(UUID userId, String sessionId) {
        StreamingSession session = sessionRepository.findBySessionId(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new StreamingException("Session not found"));
        
        return streamingMapper.toResponse(session);
    }
    
    private void configureRecording(MediaPipeline pipeline, WebRtcEndpoint endpoint, UUID userId, StreamingRequest request) {
        try {
            // Create recorder endpoint
            String recordingPath = String.format("/recordings/%s/%s_%s.webm",
                    userId,
                    request.getCharacterId(),
                    System.currentTimeMillis()
            );
            
            RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, "file://" + recordingPath).build();
            
            // Connect WebRTC endpoint to recorder
            endpoint.connect(recorder);
            
            // Start recording
            recorder.record();
            
            log.info("Recording started for session");
            
        } catch (Exception e) {
            log.error("Failed to configure recording: {}", e.getMessage());
        }
    }
    
    private void publishStreamingEvent(StreamingSession session, String eventType) {
        Map<String, Object> event = Map.of(
                "sessionId", session.getSessionId(),
                "userId", session.getUserId(),
                "characterId", session.getCharacterId(),
                "eventType", eventType,
                "timestamp", LocalDateTime.now()
        );
        
        kafkaTemplate.send("streaming-events", "streaming." + eventType, event);
    }
    
    private void publishIceCandidate(String userId, IceCandidate candidate) {
        Map<String, Object> event = Map.of(
                "userId", userId,
                "type", "ice-candidate",
                "candidate", candidate,
                "timestamp", LocalDateTime.now()
        );
        
        kafkaTemplate.send("websocket-events", "ice.candidate", event);
    }
}

// TranscodingService.java
package com.virtualcompanion.mediaservice.service;

import com.virtualcompanion.mediaservice.dto.TranscodeRequest;
import com.virtualcompanion.mediaservice.entity.MediaFile;

import java.util.UUID;

public interface TranscodingService {
    UUID startTranscodingJob(MediaFile mediaFile, TranscodeRequest request);
    void startAsyncTranscoding(UUID mediaFileId, String preset);
    byte[] generateThumbnail(byte[] videoData);
}

// FFmpegTranscodingService.java
package com.virtualcompanion.mediaservice.service.impl;

import com.virtualcompanion.mediaservice.dto.TranscodeRequest;
import com.virtualcompanion.mediaservice.entity.MediaFile;
import com.virtualcompanion.mediaservice.entity.MediaVariant;
import com.virtualcompanion.mediaservice.exception.TranscodingException;
import com.virtualcompanion.mediaservice.repository.MediaFileRepository;
import com.virtualcompanion.mediaservice.repository.MediaVariantRepository;
import com.virtualcompanion.mediaservice.service.StorageService;
import com.virtualcompanion.mediaservice.service.TranscodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FFmpegTranscodingService implements TranscodingService {
    
    private final MediaFileRepository mediaFileRepository;
    private final MediaVariantRepository variantRepository;
    private final StorageService storageService;
    
    @Value("${ffmpeg.path}")
    private String ffmpegPath;
    
    @Value("${ffmpeg.threads}")
    private int ffmpegThreads;
    
    @Value("${ffmpeg.timeout}")
    private long ffmpegTimeout;
    
    @Value("${ffmpeg.presets}")
    private Map<String, Map<String, String>> presets;
    
    @Override
    public UUID startTranscodingJob(MediaFile mediaFile, TranscodeRequest request) {
        UUID jobId = UUID.randomUUID();
        
        // Start async transcoding
        CompletableFuture.runAsync(() -> {
            try {
                processTranscoding(mediaFile, request, jobId);
            } catch (Exception e) {
                log.error("Transcoding failed for job {}: {}", jobId, e.getMessage());
                mediaFileRepository.updateProcessingStatus(mediaFile.getId(), "transcode_failed");
            }
        });
        
        return jobId;
    }
    
    @Override
    @Async
    public void startAsyncTranscoding(UUID mediaFileId, String preset) {
        try {
            MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                    .orElseThrow(() -> new TranscodingException("Media file not found"));
            
            TranscodeRequest request = TranscodeRequest.builder()
                    .qualityPreset(preset)
                    .generateThumbnails(true)
                    .thumbnailCount(5)
                    .build();
            
            processTranscoding(mediaFile, request, UUID.randomUUID());
            
        } catch (Exception e) {
            log.error("Async transcoding failed for media {}: {}", mediaFileId, e.getMessage());
        }
    }
    
    @Override
    public byte[] generateThumbnail(byte[] videoData) {
        try {
            // Save video data to temp file
            Path tempVideo = Files.createTempFile("video_", ".tmp");
            Files.write(tempVideo, videoData);
            
            // Generate thumbnail
            Path tempThumbnail = Files.createTempFile("thumb_", ".jpg");
            
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(tempVideo.toString());
            command.add("-ss");
            command.add("00:00:01"); // Take frame at 1 second
            command.add("-frames:v");
            command.add("1");
            command.add("-vf");
            command.add("scale=320:180");
            command.add("-y");
            command.add(tempThumbnail.toString());
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TranscodingException("Thumbnail generation timed out");
            }
            
            if (process.exitValue() != 0) {
                throw new TranscodingException("FFmpeg failed with exit code: " + process.exitValue());
            }
            
            byte[] thumbnailData = Files.readAllBytes(tempThumbnail);
            
            // Clean up temp files
            Files.deleteIfExists(tempVideo);
            Files.deleteIfExists(tempThumbnail);
            
            return thumbnailData;
            
        } catch (Exception e) {
            log.error("Failed to generate thumbnail: {}", e.getMessage());
            throw new TranscodingException("Failed to generate thumbnail: " + e.getMessage());
        }
    }
    
    private void processTranscoding(MediaFile mediaFile, TranscodeRequest request, UUID jobId) {
        log.info("Starting transcoding job {} for media {}", jobId, mediaFile.getId());
        
        try {
            // Update status
            mediaFileRepository.updateProcessingStatus(mediaFile.getId(), "transcoding");
            
            // Download original file
            byte[] originalData = storageService.getFileContent(mediaFile.getStoragePath());
            Path tempInput = Files.createTempFile("input_", getFileExtension(mediaFile.getFileName()));
            Files.write(tempInput, originalData);
            
            // Get preset settings
            Map<String, String> presetSettings = presets.get(request.getQualityPreset());
            if (presetSettings == null) {
                presetSettings = presets.get("medium"); // Default preset
            }
            
            // Transcode to requested format
            Path tempOutput = Files.createTempFile("output_", ".mp4");
            
            List<String> command = buildFFmpegCommand(
                    tempInput.toString(),
                    tempOutput.toString(),
                    presetSettings,
                    request
            );
            
            executeFFmpegCommand(command);
            
            // Upload transcoded file
            byte[] transcodedData = Files.readAllBytes(tempOutput);
            String variantPath = mediaFile.getStoragePath().replace(".", "_" + request.getQualityPreset() + ".");
            storageService.uploadBytes(transcodedData, variantPath);
            
            // Save variant record
            MediaVariant variant = MediaVariant.builder()
                    .mediaFileId(mediaFile.getId())
                    .variantType("transcode")
                    .quality(request.getQualityPreset())
                    .format("mp4")
                    .storagePath(variantPath)
                    .fileSize((long) transcodedData.length)
                    .width(Integer.parseInt(presetSettings.get("resolution").split("x")[0]))
                    .height(Integer.parseInt(presetSettings.get("resolution").split("x")[1]))
                    .bitrate(parseBitrate(presetSettings.get("bitrate")))
                    .build();
            
            variantRepository.save(variant);
            
            // Generate thumbnails if requested
            if (Boolean.TRUE.equals(request.getGenerateThumbnails())) {
                generateVideoThumbnails(mediaFile, tempInput, request.getThumbnailCount());
            }
            
            // Clean up temp files
            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempOutput);
            
            // Update status
            mediaFileRepository.updateProcessingStatus(mediaFile.getId(), "completed");
            
            log.info("Transcoding completed for job {}", jobId);
            
        } catch (Exception e) {
            log.error("Transcoding failed for job {}: {}", jobId, e.getMessage());
            throw new TranscodingException("Transcoding failed: " + e.getMessage());
        }
    }
    
    private List<String> buildFFmpegCommand(String input, String output, 
                                           Map<String, String> preset, 
                                           TranscodeRequest request) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(input);
        command.add("-threads");
        command.add(String.valueOf(ffmpegThreads));
        
        // Video codec
        command.add("-c:v");
        command.add(preset.get("video-codec"));
        
        // Audio codec
        command.add("-c:a");
        command.add(preset.get("audio-codec"));
        
        // Bitrate
        command.add("-b:v");
        command.add(preset.get("bitrate"));
        
        // Resolution
        command.add("-vf");
        command.add("scale=" + preset.get("resolution"));
        
        // Additional options
        command.add("-preset");
        command.add("fast");
        command.add("-movflags");
        command.add("+faststart");
        
        command.add("-y"); // Overwrite output
        command.add(output);
        
        return command;
    }
    
    private void executeFFmpegCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // Log output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }
        
        boolean finished = process.waitFor(ffmpegTimeout, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new TranscodingException("FFmpeg process timed out");
        }
        
        if (process.exitValue() != 0) {
            throw new TranscodingException("FFmpeg failed with exit code: " + process.exitValue());
        }
    }
    
    private void generateVideoThumbnails(MediaFile mediaFile, Path videoPath, Integer count) {
        // Implementation would generate multiple thumbnails at different timestamps
        log.info("Generating {} thumbnails for media {}", count, mediaFile.getId());
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
    
    private Integer parseBitrate(String bitrate) {
        // Parse bitrate string like "1000k" to integer
        if (bitrate.endsWith("k")) {
            return Integer.parseInt(bitrate.substring(0, bitrate.length() - 1));
        }
        return Integer.parseInt(bitrate);
    }
}