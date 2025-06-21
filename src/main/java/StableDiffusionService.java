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
