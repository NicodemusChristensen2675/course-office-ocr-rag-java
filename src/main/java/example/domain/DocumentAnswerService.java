package example.domain;

import example.infra.InfraiClient;
import java.io.IOException;

public final class DocumentAnswerService {
    private final InfraiClient client;
    public DocumentAnswerService(InfraiClient client) { this.client = client; }
    public String indexAndAnswer(byte[] pdf, String tenantId, String accountId, String question) throws Exception {
        String ocrEnvelope = client.ocr(pdf);
        String text = extract(ocrEnvelope, "text");
        if (text.isBlank()) throw new IllegalArgumentException("OCR produced no document text");
        String embedding = extract(client.embeddings(text), "embedding");
        String metadata = "{\"tenant_id\":\"" + tenantId + "\",\"account_id\":\"" + accountId + "\",\"text\":\"" + text.replace("\"", "'") + "\"}";
        client.upsert("{\"collection\":\"course-office\",\"vectors\":[{\"id\":\"" + accountId + "-document\",\"embedding\":" + embedding + ",\"metadata\":" + metadata + "}]}");
        return client.query("{\"collection\":\"course-office\",\"embedding\":" + extract(client.embeddings(question), "embedding") + ",\"top_k\":1,\"include_metadata\":true}");
    }
    public static String extract(String json, String field) {
        String marker = "\"" + field + "\":"; int start = json.indexOf(marker); if (start < 0) return "";
        start += marker.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int depth = 0; boolean inString = false; boolean escaped = false; int end = start;
        for (; end < json.length(); end++) {
            char c = json.charAt(end);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
            } else if (c == '"') inString = true;
            else if (c == '[' || c == '{') depth++;
            else if (c == ']' || c == '}') {
                if (depth == 0) break;
                depth--;
            } else if (c == ',' && depth == 0) break;
        }
        String value = json.substring(start, end).trim();
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
