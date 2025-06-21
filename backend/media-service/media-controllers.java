// MediaController.java
package com.virtualcompanion.mediaservice.controller;

import com.virtualcompanion.mediaservice.dto.*;
import com.virtualcompanion.mediaservice.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class MediaController {
    
    private final MediaService mediaService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload media file")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MediaUploadResponse> uploadMedia(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute MediaUploadRequest request) {
        
        MediaUploadResponse response = mediaService.uploadMedia(userId, file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media details")
    public ResponseEntity<MediaUploadResponse> getMedia(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID mediaId) {
        
        MediaUploadResponse response = mediaService.getMediaById(userId, mediaId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID mediaId) {
        
        mediaService.deleteMedia(userId, mediaId);
    }
    
    @GetMapping
    @Operation(summary = "Get user's media files")
    public ResponseEntity<Page<MediaUploadResponse>> getUserMedia(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<MediaUploadResponse> media = mediaService.getUserMedia(userId, pageRequest);
        return ResponseEntity.ok(media);
    }
    
    @PostMapping("/search")
    @Operation(summary = "Search media files")
    public ResponseEntity<Page<MediaUploadResponse>> searchMedia(
            @Valid @RequestBody MediaSearchRequest request) {
        
        Page<MediaUploadResponse> results = mediaService.searchMedia(request);
        return ResponseEntity.ok(results);
    }
    
    @PostMapping("/{mediaId}/transcode")
    @Operation(summary = "Transcode video file")
    public ResponseEntity<TranscodeResponse> transcodeMedia(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID mediaId,
            @Valid @RequestBody TranscodeRequest request) {
        
        TranscodeResponse response = mediaService.transcodeMedia(userId, mediaId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get user media statistics")
    public ResponseEntity<MediaStatisticsResponse> getStatistics(
            @AuthenticationPrincipal UUID userId) {
        
        MediaStatisticsResponse statistics = mediaService.getUserMediaStatistics(userId);
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/{mediaId}/content")
    @Operation(summary = "Get media file content")
    public ResponseEntity<Resource> getMediaContent(
            @PathVariable UUID mediaId,
            @RequestParam(required = false) String variant) {
        
        byte[] content = mediaService.getMediaContent(mediaId, variant);
        ByteArrayResource resource = new ByteArrayResource(content);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentLength(content.length)
                .body(resource);
    }
    
    @GetMapping("/{mediaId}/thumbnail")
    @Operation(summary = "Get media thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable UUID mediaId) {
        
        byte[] thumbnail = mediaService.getThumbnail(mediaId);
        ByteArrayResource resource = new ByteArrayResource(thumbnail);
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(thumbnail.length)
                .body(resource);
    }
}

// VoiceController.java
package com.virtualcompanion.mediaservice.controller;

import com.virtualcompanion.mediaservice.dto.VoiceGenerationRequest;
import com.virtualcompanion.mediaservice.dto.VoiceGenerationResponse;
import com.virtualcompanion.mediaservice.service.VoiceGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
@Tag(name = "Voice Generation", description = "Voice generation endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class VoiceController {
    
    private final VoiceGenerationService voiceGenerationService;
    
    @PostMapping("/generate")
    @Operation(summary = "Generate voice from text")
    public ResponseEntity<VoiceGenerationResponse> generateVoice(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody VoiceGenerationRequest request) {
        
        VoiceGenerationResponse response = voiceGenerationService.generateVoice(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/generations/{generationId}")
    @Operation(summary = "Get voice generation details")
    public ResponseEntity<VoiceGenerationResponse> getVoiceGeneration(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID generationId) {
        
        VoiceGenerationResponse response = voiceGenerationService.getVoiceGeneration(userId, generationId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/generations")
    @Operation(summary = "Get user's voice generations")
    public ResponseEntity<Page<VoiceGenerationResponse>> getUserVoiceGenerations(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VoiceGenerationResponse> generations = voiceGenerationService.getUserVoiceGenerations(userId, pageRequest);
        return ResponseEntity.ok(generations);
    }
    
    @GetMapping("/generations/{generationId}/audio")
    @Operation(summary = "Get generated audio file")
    public ResponseEntity<Resource> getAudioContent(@PathVariable UUID generationId) {
        
        byte[] audio = voiceGenerationService.getAudioContent(generationId);
        ByteArrayResource resource = new ByteArrayResource(audio);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"voice_" + generationId + ".mp3\"")
                .contentLength(audio.length)
                .body(resource);
    }
    
    @DeleteMapping("/generations/{generationId}")
    @Operation(summary = "Delete voice generation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVoiceGeneration(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID generationId) {
        
        voiceGenerationService.deleteVoiceGeneration(userId, generationId);
    }
}

// StreamingController.java
package com.virtualcompanion.mediaservice.controller;

import com.virtualcompanion.mediaservice.dto.*;
import com.virtualcompanion.mediaservice.service.StreamingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/streaming")
@RequiredArgsConstructor
@Tag(name = "Streaming", description = "WebRTC streaming endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class StreamingController {
    
    private final StreamingService streamingService;
    
    @PostMapping("/sessions")
    @Operation(summary = "Start streaming session")
    public ResponseEntity<StreamingResponse> startSession(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody StreamingRequest request) {
        
        StreamingResponse response = streamingService.startSession(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/sessions/{sessionId}/answer")
    @Operation(summary = "Set SDP answer")
    public ResponseEntity<Void> setSdpAnswer(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String sessionId,
            @RequestBody String sdpAnswer) {
        
        streamingService.setSdpAnswer(userId, sessionId, sdpAnswer);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sessions/{sessionId}/ice-candidate")
    @Operation(summary = "Add ICE candidate")
    public ResponseEntity<Void> addIceCandidate(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String sessionId,
            @RequestBody IceCandidate candidate) {
        
        streamingService.addIceCandidate(userId, sessionId, candidate);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sessions/{sessionId}/end")
    @Operation(summary = "End streaming session")
    public ResponseEntity<Void> endSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String sessionId) {
        
        streamingService.endSession(userId, sessionId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get session details")
    public ResponseEntity<StreamingSessionResponse> getSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String sessionId) {
        
        StreamingSessionResponse response = streamingService.getSession(userId, sessionId);
        return ResponseEntity.ok(response);
    }
}