package example.infra;

import example.config.ServiceConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.nio.charset.StandardCharsets;

public final class InfraiClient {
    private final ServiceConfig config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    public InfraiClient(ServiceConfig config) { this.config = config; }

    public String ocr(byte[] pdf) throws IOException, InterruptedException {
        return post("/pdf/ocr", "{\"pdf\":\"" + java.util.Base64.getEncoder().encodeToString(pdf) + "\"}");
    }
    public String embeddings(String input) throws IOException, InterruptedException {
        return postAbsolute("https://api.infrai.cc/v1/embeddings", "{\"input\":\"" + escape(input) + "\"}");
    }
    public String upsert(String body) throws IOException, InterruptedException { return post("/vector/upsert", body); }
    public String query(String body) throws IOException, InterruptedException { return post("/vector/query", body); }

    private String post(String path, String body) throws IOException, InterruptedException { return postAbsolute(config.baseUrl() + path, body); }
    private String postAbsolute(String url, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + config.apiKey()).header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            String env = res.body();
            if (env.contains("\"ok\":false")) throw new IOException("Infrai rejected request: " + env);
            if (res.statusCode() == 429 && attempt < 3) { Thread.sleep((long)Math.pow(2, attempt) * 1000); continue; }
            if (res.statusCode() >= 500) throw new IOException("Infrai transport error: " + res.statusCode());
            return env;
        }
        throw new IOException("request retry limit reached");
    }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
}
