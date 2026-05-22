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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FairStackClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String generatePath;
    private final String jobStatusPath;
    private final String model;
    private final boolean confirm;
    private final int timeoutSeconds;

    public FairStackClient(
            ObjectMapper objectMapper,
            @Value("${fairstack.base-url:}") String baseUrl,
            @Value("${fairstack.api-key:}") String apiKey,
            @Value("${fairstack.generate-path:/generations/threeD}") String generatePath,
            @Value("${fairstack.job-status-path:/jobs/{jobId}}") String jobStatusPath,
            @Value("${fairstack.model:hunyuan-3d-rapid-t23d}") String model,
            @Value("${fairstack.confirm:false}") boolean confirm,
            @Value("${fairstack.timeout-seconds:180}") int timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = trimSlash(baseUrl);
        this.apiKey = apiKey;
        this.generatePath = normalizePath(generatePath);
        this.jobStatusPath = normalizePath(jobStatusPath);
        this.model = model;
        this.confirm = confirm;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public Map<String, Object> startTextTo3D(String prompt, Long recipeId, String assetKey) {
        validateConfiguration();

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        payload.put("output_format", "glb");
        payload.put("confirm", confirm);
        payload.put("metadata", Map.of(
                "source", "Pastry3D",
                "recipeId", recipeId,
                "assetKey", assetKey
        ));

        return postJson(baseUrl + generatePath, payload);
    }

    public Map<String, Object> getJobStatus(String jobId) {
        validateConfiguration();

        if (jobId == null || jobId.isBlank()) {
            throw new BadRequestException("El job de FairStack no tiene identificador externo");
        }

        String path = jobStatusPath.replace("{jobId}", jobId);
        return getJson(baseUrl + path);
    }

    public byte[] downloadBinary(String url) {
        if (url == null || url.isBlank()) {
            throw new BadRequestException("La URL del modelo generado está vacía");
        }

        try {
            HttpRequest requestWithoutAuth = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(requestWithoutAuth, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                HttpRequest requestWithAuth = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Authorization", "Bearer " + apiKey)
                        .GET()
                        .build();

                response = httpClient.send(requestWithAuth, HttpResponse.BodyHandlers.ofByteArray());
            }

            if (!isSuccess(response.statusCode())) {
                throw new BadRequestException("No se pudo descargar el GLB generado. Código " + response.statusCode());
            }

            return response.body();
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Error descargando GLB generado: " + exception.getMessage());
        }
    }

    public String extractProviderJobId(Map<String, Object> response) {
        Object id = firstNonNull(
                response.get("id"),
                response.get("jobId"),
                response.get("job_id"),
                response.get("taskId"),
                response.get("task_id"),
                response.get("requestId"),
                response.get("request_id")
        );

        return id == null ? null : id.toString();
    }

    public String extractModelUrl(Map<String, Object> response) {
        String direct = firstString(
                response.get("modelUrl"),
                response.get("model_url"),
                response.get("outputUrl"),
                response.get("output_url"),
                response.get("assetUrl"),
                response.get("asset_url"),
                response.get("fileUrl"),
                response.get("file_url"),
                response.get("downloadUrl"),
                response.get("download_url")
        );

        if (direct != null) {
            return direct;
        }

        Object output = response.get("output");
        if (output instanceof Map<?, ?> outputMap) {
            String nested = firstString(
                    outputMap.get("modelUrl"),
                    outputMap.get("model_url"),
                    outputMap.get("assetUrl"),
                    outputMap.get("asset_url"),
                    outputMap.get("url"),
                    outputMap.get("downloadUrl"),
                    outputMap.get("download_url")
            );

            if (nested != null) {
                return nested;
            }
        }

        Object file = response.get("file");
        if (file instanceof Map<?, ?> fileMap) {
            String nested = firstString(
                    fileMap.get("url"),
                    fileMap.get("downloadUrl"),
                    fileMap.get("download_url")
            );

            if (nested != null) {
                return nested;
            }
        }

        Object results = response.get("results");
        if (results instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> resultMap) {
                return firstString(
                        resultMap.get("asset_url"),
                        resultMap.get("assetUrl"),
                        resultMap.get("model_url"),
                        resultMap.get("modelUrl"),
                        resultMap.get("url"),
                        resultMap.get("download_url"),
                        resultMap.get("downloadUrl")
                );
            }
        }

        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            return firstString(
                    dataMap.get("asset_url"),
                    dataMap.get("assetUrl"),
                    dataMap.get("model_url"),
                    dataMap.get("modelUrl"),
                    dataMap.get("url"),
                    dataMap.get("download_url"),
                    dataMap.get("downloadUrl")
            );
        }

        return null;
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
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BadRequestException("Falta fairstack.base-url");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("Falta fairstack.api-key");
        }

        if (model == null || model.isBlank()) {
            throw new BadRequestException("Falta fairstack.model");
        }
    }

    private boolean isSuccess(int code) {
        return code >= 200 && code < 300;
    }

    private String trimSlash(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.trim();

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return normalized;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String firstString(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }

        return null;
    }
}