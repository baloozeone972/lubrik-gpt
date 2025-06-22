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
