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
