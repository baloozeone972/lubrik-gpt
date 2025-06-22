class LLMConfig {
    private String provider = "ollama"; // ollama, localai, huggingface
    private String baseUrl = "http://localhost:11434";
    private String model = "llama2:13b-chat";
    private int maxTokens = 2048;
    private double temperature = 0.7;
}
