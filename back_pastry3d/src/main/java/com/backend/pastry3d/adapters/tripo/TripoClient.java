package com.backend.pastry3d.adapters.tripo;

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
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TripoClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String modelVersion;
    private final String textureQuality;
    private final String geometryQuality;
    private final String negativePrompt;
    private final boolean texture;
    private final boolean pbr;
    private final boolean autoSize;
    private final boolean exportUv;
    private final boolean smartLowPoly;
    private final boolean quad;
    private final int faceLimit;
    private final int timeoutSeconds;

    public TripoClient(
            ObjectMapper objectMapper,
            @Value("${tripo.base-url:https://api.tripo3d.ai/v2/openapi}") String baseUrl,
            @Value("${tripo.api-key:}") String apiKey,
            @Value("${tripo.model-version:v3.1-20260211}") String modelVersion,
            @Value("${tripo.texture:true}") boolean texture,
            @Value("${tripo.pbr:true}") boolean pbr,
            @Value("${tripo.texture-quality:standard}") String textureQuality,
            @Value("${tripo.geometry-quality:standard}") String geometryQuality,
            @Value("${tripo.face-limit:30000}") int faceLimit,
            @Value("${tripo.auto-size:true}") boolean autoSize,
            @Value("${tripo.export-uv:true}") boolean exportUv,
            @Value("${tripo.smart-low-poly:false}") boolean smartLowPoly,
            @Value("${tripo.quad:false}") boolean quad,
            @Value("${tripo.negative-prompt:low quality, blurry, deformed, broken geometry, extra objects, plate, table, hands, people, text, logo, watermark}") String negativePrompt,
            @Value("${tripo.timeout-seconds:180}") int timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = trimSlash(baseUrl);
        this.apiKey = apiKey;
        this.modelVersion = modelVersion;
        this.texture = texture;
        this.pbr = pbr;
        this.textureQuality = textureQuality;
        this.geometryQuality = geometryQuality;
        this.faceLimit = faceLimit;
        this.autoSize = autoSize;
        this.exportUv = exportUv;
        this.smartLowPoly = smartLowPoly;
        this.quad = quad;
        this.negativePrompt = negativePrompt;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public Map<String, Object> startTextToModel(String prompt, Long recipeId, String assetKey) {
        validateConfiguration();

        String safePrompt = sanitizePrompt(prompt);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "text_to_model");
        payload.put("prompt", safePrompt);
        payload.put("model_version", modelVersion);
        payload.put("negative_prompt", limitText(negativePrompt, 255));
        payload.put("texture", texture);
        payload.put("pbr", pbr);
        payload.put("geometry_quality", safeEnum(geometryQuality, "standard"));
        payload.put("auto_size", autoSize);
        payload.put("export_uv", exportUv);
        payload.put("smart_low_poly", smartLowPoly);
        payload.put("quad", quad);

        if (texture || pbr) {
            payload.put("texture_quality", safeEnum(textureQuality, "standard"));
        }

        if (faceLimit > 0) {
            payload.put("face_limit", faceLimit);
        }

        payload.put("metadata", Map.of(
                "source", "Pastry3D",
                "recipeId", recipeId,
                "assetKey", assetKey
        ));

        return postJson(baseUrl + "/task", payload);
    }

    public Map<String, Object> getTask(String taskId) {
        validateConfiguration();

        if (taskId == null || taskId.isBlank()) {
            throw new BadRequestException("El job de Tripo no tiene identificador externo");
        }

        return getJson(baseUrl + "/task/" + taskId.trim());
    }

    public byte[] downloadBinary(String url) {
        if (url == null || url.isBlank()) {
            throw new BadRequestException("La URL del modelo generado está vacía");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (!isSuccess(response.statusCode())) {
                throw new BadRequestException("No se pudo descargar el GLB generado por Tripo. Código " + response.statusCode());
            }

            return response.body();
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Error descargando GLB generado por Tripo: " + exception.getMessage());
        }
    }

    public String extractTaskId(Map<String, Object> response) {
        Object direct = firstNonNull(
                response.get("task_id"),
                response.get("taskId"),
                response.get("id")
        );

        if (direct != null) {
            return direct.toString();
        }

        Map<String, Object> data = mapValue(response.get("data"));
        Object nested = firstNonNull(
                data.get("task_id"),
                data.get("taskId"),
                data.get("id")
        );

        return nested == null ? null : nested.toString();
    }

    public String extractStatus(Map<String, Object> response) {
        Object direct = firstNonNull(response.get("status"), response.get("state"));

        if (direct != null) {
            return direct.toString();
        }

        Map<String, Object> data = mapValue(response.get("data"));
        Object nested = firstNonNull(data.get("status"), data.get("state"));

        return nested == null ? null : nested.toString();
    }

    public String extractModelUrl(Map<String, Object> response) {
        String direct = firstString(
                response.get("pbr_model"),
                response.get("model"),
                response.get("base_model"),
                response.get("model_url"),
                response.get("modelUrl")
        );

        if (direct != null) {
            return direct;
        }

        Map<String, Object> data = mapValue(response.get("data"));
        String fromData = firstString(
                data.get("pbr_model"),
                data.get("model"),
                data.get("base_model"),
                data.get("model_url"),
                data.get("modelUrl")
        );

        if (fromData != null) {
            return fromData;
        }

        Map<String, Object> output = mapValue(firstNonNull(response.get("output"), data.get("output")));
        return firstString(
                output.get("pbr_model"),
                output.get("model"),
                output.get("base_model"),
                output.get("model_url"),
                output.get("modelUrl"),
                output.get("glb"),
                output.get("url")
        );
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
                throw new BadRequestException("Tripo respondió con código " + response.statusCode() + ": " + response.body());
            }

            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Error llamando Tripo: " + exception.getMessage());
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
                throw new BadRequestException("Tripo respondió con código " + response.statusCode() + ": " + response.body());
            }

            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Error consultando Tripo: " + exception.getMessage());
        }
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("TRIPO_API_KEY no está configurada");
        }

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BadRequestException("TRIPO_BASE_URL no está configurada");
        }
    }

    private String sanitizePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new BadRequestException("El prompt para Tripo está vacío");
        }

        String clean = prompt
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
                .replaceAll("[\\uD800-\\uDFFF]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return limitText(clean, 1024);
    }

    private String limitText(String value, int max) {
        if (value == null) {
            return "";
        }

        String text = value.trim();
        return text.length() <= max ? text : text.substring(0, max).trim();
    }

    private String safeEnum(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim().toLowerCase();
    }

    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }

        return new LinkedHashMap<>();
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
            if (value == null) {
                continue;
            }

            String text = value.toString().trim();
            if (!text.isBlank()) {
                return text;
            }
        }

        return null;
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
}