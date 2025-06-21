package com.virtualcompanion.mediaservice.controller;

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
