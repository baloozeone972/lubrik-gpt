public class CoquiVoiceService implements VoiceGenerationService {
    
    private final WebClient ttsClient;
    private final AIConfiguration config;
    
    @Override
    public Mono<byte[]> generateSpeech(VoiceRequest request) {
        CoquiTTSRequest ttsRequest = CoquiTTSRequest.builder()
            .text(request.getText())
            .speakerId(request.getCharacterVoiceId())
            .language(request.getLanguage())
            .emotion(request.getEmotion()) // "neutral", "happy", "sad", "angry"
            .speed(request.getSpeed())
            .build();
        
        return ttsClient.post()
            .uri("/api/tts")
            .body(Mono.just(ttsRequest), CoquiTTSRequest.class)
            .retrieve()
            .bodyToMono(byte[].class)
            .doOnError(error -> log.error("Erreur génération voix: ", error));
    }
    
    @Override
    public Mono<String> cloneVoice(VoiceCloneRequest request) {
        // Upload des échantillons audio pour cloner une voix
        return ttsClient.post()
            .uri("/api/voice-clone")
            .body(BodyInserters.fromMultipartData("audio", request.getAudioSample()))
            .retrieve()
            .bodyToMono(VoiceCloneResponse.class)
            .map(VoiceCloneResponse::getVoiceId);
    }
}
