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
