package com.virtualcompanion.mediaservice.service.impl;

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
