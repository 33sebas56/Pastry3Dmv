package com.backend.pastry3d.adapters.fairstack;

import com.backend.pastry3d.shared.exception.BadRequestException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class FairStackClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String generatePath;
    private final String jobStatusPath;
    private final int timeoutSeconds;

    public FairStackClient(
            ObjectMapper objectMapper,
            @Value("${fairstack.base-url:}") String baseUrl,
            @Value("${fairstack.api-key:}") String apiKey,
            @Value("${fairstack.generate-path:/generate}") String generatePath,
            @Value("${fairstack.job-status-path:/jobs/{jobId}}") String jobStatusPath,
            @Value("${fairstack.timeout-seconds:120}") int timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = trimSlash(baseUrl);
        this.apiKey = apiKey;
        this.generatePath = generatePath;
        this.jobStatusPath = jobStatusPath;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
    }

    public Map<String, Object> startGeneration(Map<String, Object> payload) {
        validateConfiguration();
        return postJson(baseUrl + generatePath, payload);
    }

    public Map<String, Object> getJobStatus(String jobId) {
        validateConfiguration();
        String path = jobStatusPath.replace("{jobId}", jobId);
        return getJson(baseUrl + path);
    }

    private Map<String, Object> postJson(String url, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (!isSuccess(response.statusCode())) {
                throw new BadRequestException("FairStack respondió con código " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Error llamando FairStack: " + exception.getMessage());
        }
    }

    private Map<String, Object> getJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (!isSuccess(response.statusCode())) {
                throw new BadRequestException("FairStack status respondió con código " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Error consultando FairStack: " + exception.getMessage());
        }
    }

    private void validateConfiguration() {
        if (baseUrl == null || baseUrl.isBlank()) throw new BadRequestException("Falta fairstack.base-url");
        if (apiKey == null || apiKey.isBlank()) throw new BadRequestException("Falta fairstack.api-key");
    }

    private boolean isSuccess(int code) { return code >= 200 && code < 300; }

    private String trimSlash(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }
}
