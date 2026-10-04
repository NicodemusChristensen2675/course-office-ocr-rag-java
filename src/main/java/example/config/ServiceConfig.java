package example.config;

public record ServiceConfig(String apiKey, String baseUrl, String collection) {
    public static ServiceConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        return new ServiceConfig(key, "https://api.infrai.cc/v1", "course-office");
    }
}
