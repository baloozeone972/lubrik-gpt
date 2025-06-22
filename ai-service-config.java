// 1. Configuration principale des services IA
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIConfiguration {

    private LLMConfig llm = new LLMConfig();
    private ImageGenConfig imageGen = new ImageGenConfig();
    private VideoGenConfig videoGen = new VideoGenConfig();
    private VoiceGenConfig voiceGen = new VoiceGenConfig();

    @Data
    public static class LLMConfig {
        private String provider = "ollama"; // ollama, localai, huggingface
        private String baseUrl = "http://localhost:11434";
        private String model = "llama2:13b-chat";
        private int maxTokens = 2048;
        private double temperature = 0.7;
    }

    @Data
    public static class ImageGenConfig {
        private String baseUrl = "http://localhost:7860";
        private String model = "sdxl_base_1.0";
        private int width = 512;
        private int height = 512;
        private int steps = 30;
    }

    @Data
    public static class VideoGenConfig {
        private String baseUrl = "http://localhost:5003";
        private String model = "stable-video-diffusion";
        private int fps = 8;
        private int duration = 4; // seconds
    }

    @Data
    public static class VoiceGenConfig {
        private String baseUrl = "http://localhost:5002";
        private String model = "xtts_v2";
        private String language = "fr";
    }
}

// 2. Service LLM avec Ollama
@Service
@Slf4j
public class OllamaLLMService implements LLMService {

    private final WebClient webClient;
    private final AIConfiguration config;
    private final RedisTemplate<String, String> cacheTemplate;

    public OllamaLLMService(AIConfiguration config, RedisTemplate<String, String> cacheTemplate) {
        this.config = config;
        this.cacheTemplate = cacheTemplate;
        this.webClient = WebClient.builder()
                .baseUrl(config.getLlm().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Mono<String> generateResponse(ConversationContext context) {
        String cacheKey = generateCacheKey(context);

        // Vérifier le cache
        String cachedResponse = cacheTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            return Mono.just(cachedResponse);
        }

        OllamaRequest request = OllamaRequest.builder()
                .model(config.getLlm().getModel())
                .prompt(buildPrompt(context))
                .system(context.getCharacter().getSystemPrompt())
                .temperature(config.getLlm().getTemperature())
                .maxTokens(config.getLlm().getMaxTokens())
                .stream(false)
                .build();

        return webClient.post()
                .uri("/api/generate")
                .body(Mono.just(request), OllamaRequest.class)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .map(response -> {
                    String text = response.getResponse();
                    // Cache pour 1 heure
                    cacheTemplate.opsForValue().set(cacheKey, text, Duration.ofHours(1));
                    return text;
                })
                .timeout(Duration.ofSeconds(30))
                .doOnError(error -> log.error("Erreur génération LLM: ", error));
    }

    @Override
    public Flux<String> streamResponse(ConversationContext context) {
        OllamaRequest request = OllamaRequest.builder()
                .model(config.getLlm().getModel())
                .prompt(buildPrompt(context))
                .system(context.getCharacter().getSystemPrompt())
                .temperature(config.getLlm().getTemperature())
                .stream(true)
                .build();

        return webClient.post()
                .uri("/api/generate")
                .body(Mono.just(request), OllamaRequest.class)
                .retrieve()
                .bodyToFlux(String.class)
                .map(chunk -> {
                    // Parser le JSON streaming
                    JsonNode node = objectMapper.readTree(chunk);
                    return node.get("response").asText();
                })
                .filter(text -> !text.isEmpty());
    }

    private String buildPrompt(ConversationContext context) {
        StringBuilder prompt = new StringBuilder();

        // Inclure l'historique de conversation
        context.getRecentMessages().forEach(msg -> {
            prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        });

        // Ajouter le message actuel
        prompt.append("User: ").append(context.getCurrentMessage()).append("\n");
        prompt.append("Assistant: ");

        return prompt.toString();
    }
}

// 3. Service Génération d'Images avec Stable Diffusion
@Service
@Slf4j
public class StableDiffusionService implements ImageGenerationService {

    private final WebClient webClient;
    private final AIConfiguration config;
    private final MinioClient minioClient;

    @Override
    public Mono<GeneratedImage> generateCharacterImage(CharacterImageRequest request) {
        StableDiffusionRequest sdRequest = StableDiffusionRequest.builder()
                .prompt(buildCharacterPrompt(request))
                .negativePrompt("nsfw, nude, sexual, violence, gore, deformed")
                .width(config.getImageGen().getWidth())
                .height(config.getImageGen().getHeight())
                .steps(config.getImageGen().getSteps())
                .cfgScale(7.5)
                .sampler("DPM++ 2M Karras")
                .model(config.getImageGen().getModel())
                .build();

        return webClient.post()
                .uri("/sdapi/v1/txt2img")
                .body(Mono.just(sdRequest), StableDiffusionRequest.class)
                .retrieve()
                .bodyToMono(StableDiffusionResponse.class)
                .flatMap(response -> saveToStorage(response, request.getCharacterId()))
                .doOnError(error -> log.error("Erreur génération image: ", error));
    }

    private String buildCharacterPrompt(CharacterImageRequest request) {
        CharacterAppearance appearance = request.getAppearance();

        return String.format(
                "portrait of %s, %s years old, %s hair, %s eyes, %s, %s, " +
                        "high quality, detailed, professional photography, studio lighting",
                appearance.getGender(),
                appearance.getAge(),
                appearance.getHairColor(),
                appearance.getEyeColor(),
                appearance.getBodyType(),
                appearance.getStyle()
        );
    }
}

// 4. Service Génération Vidéo
@Service
@Slf4j
public class VideoGenerationService {

    private final WebClient videoGenClient;
    private final AIConfiguration config;

    @Override
    public Mono<GeneratedVideo> generateCharacterVideo(VideoRequest request) {
        // Appel au service Python de génération vidéo
        VideoGenRequest genRequest = VideoGenRequest.builder()
                .imageUrl(request.getSourceImageUrl())
                .motion(request.getMotionType()) // "talking", "waving", "nodding"
                .duration(config.getVideoGen().getDuration())
                .fps(config.getVideoGen().getFps())
                .build();

        return videoGenClient.post()
                .uri("/generate")
                .body(Mono.just(genRequest), VideoGenRequest.class)
                .retrieve()
                .bodyToMono(VideoGenResponse.class)
                .map(response -> GeneratedVideo.builder()
                        .videoUrl(response.getVideoUrl())
                        .thumbnailUrl(response.getThumbnailUrl())
                        .duration(response.getDuration())
                        .build())
                .timeout(Duration.ofMinutes(5));
    }
}

// 5. Service Génération de Voix avec Coqui TTS
@Service
@Slf4j
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