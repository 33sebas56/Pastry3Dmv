package com.backend.pastry3d.ai.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GeminiClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public GeminiClient(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-1.5-flash}") String model,
            @Value("${gemini.enabled:false}") boolean enabled
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public Map<String, Object> generateRecipePlan(String userPrompt) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return fallbackPlan(userPrompt);
        }

        try {
            String prompt = buildPrompt(userPrompt);

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.35);
            generationConfig.put("topP", 0.9);
            generationConfig.put("maxOutputTokens", 4096);
            generationConfig.put("responseMimeType", "application/json");

            Map<String, Object> request = new HashMap<>();
            request.put("contents", List.of(
                    Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", prompt))
                    )
            ));
            request.put("generationConfig", generationConfig);

            String body = objectMapper.writeValueAsString(request);

            URI uri = URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + model
                            + ":generateContent?key="
                            + apiKey
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(70))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallbackPlan(userPrompt);
            }

            Map<String, Object> raw = objectMapper.readValue(response.body(), new TypeReference<>() {});
            String text = extractText(raw);

            if (text == null || text.isBlank()) {
                return fallbackPlan(userPrompt);
            }

            String cleanJson = cleanJsonText(text);
            Map<String, Object> parsed = objectMapper.readValue(cleanJson, new TypeReference<>() {});

            if (!parsed.containsKey("recipe") || !parsed.containsKey("visualPlan")) {
                return fallbackPlan(userPrompt);
            }

            return parsed;
        } catch (Exception exception) {
            return fallbackPlan(userPrompt);
        }
    }

    private String buildPrompt(String userPrompt) {
        return """
                Eres el director creativo y chef técnico de Pastry3D.

                Tu tarea:
                1. Entender el pedido del usuario.
                2. Generar una receta realista, detallada y útil en español.
                3. Generar un plan visual estructurado para que el backend pueda buscar modelos GLB existentes.
                4. NO inventar que ya existe un archivo GLB.
                5. NO decir que generaste un modelo 3D.
                6. Solo debes devolver JSON válido, sin markdown, sin explicación, sin texto adicional.

                La arquitectura del sistema:
                - Gemini genera la receta y el plan visual.
                - El backend busca assets GLB existentes.
                - El backend compone la escena con base + toppings + decoraciones.
                - Si falta un asset, el backend lo marca como pendiente.

                Tipos de postre permitidos para "dessertType":
                - round_cake
                - milhojas
                - cupcake
                - cheesecake
                - tart
                - brownie
                - cookie
                - donut
                - macaron
                - tiramisu
                - flan
                - alfajor
                - tres_leches
                - generic_dessert

                Categorías visuales permitidas:
                - BASE
                - TOPPING
                - DECORATION
                - SAUCE
                - SPRINKLES

                Assets conocidos actualmente o esperados:
                - round_cake_base_vanilla: base de pastel redondo de vainilla
                - milhojas_base_classic: base de milhojas clásica
                - caramel_rose: rosa de caramelo o decoración tipo rosa
                - strawberry_topping: fresa decorativa
                - birthday_candle: vela de cumpleaños
                - chocolate_drizzle: salsa o baño de chocolate
                - cream_swirl: copo o decoración de crema
                - rainbow_sprinkles: chispas de colores

                Reglas importantes:
                - El postre principal debe ir en recipe.dessertType y visualPlan.dessertType.
                - Si el usuario pide "milhojas con rosa", dessertType debe ser "milhojas".
                - La rosa debe ir en visualPlan.requiredAssets como decoración o topping.
                - Si el usuario pide color azul, rojo, rosa, amarillo, verde, morado, blanco, negro o chocolate, usa color HEX.
                - Para rosas, usa materialTarget "petals".
                - Para velas, usa materialTarget "primary".
                - Para crema, usa materialTarget "cream".
                - Para chocolate, usa materialTarget "chocolate".
                - La receta debe ser de repostería realista, no genérica.
                - Los pasos deben ser claros y ordenados.
                - estimatedMinutes debe ser razonable.
                - ingredients debe tener cantidades y unidades.

                Devuelve exactamente esta estructura:

                {
                  "recipe": {
                    "title": "string",
                    "dessertType": "string",
                    "description": "string",
                    "servings": 0,
                    "difficulty": "BEGINNER|INTERMEDIATE|ADVANCED",
                    "estimatedMinutes": 0,
                    "ingredients": [
                      {
                        "name": "string",
                        "quantity": "string",
                        "unit": "string"
                      }
                    ],
                    "steps": [
                      {
                        "order": 1,
                        "title": "string",
                        "description": "string"
                      }
                    ],
                    "learningTips": [
                      "string"
                    ]
                  },
                  "visualPlan": {
                    "strategy": "COMPOSE_SCENE",
                    "normalizedPrompt": "string",
                    "dessertType": "string",
                    "keywords": ["string"],
                    "baseAssetQuery": "string",
                    "requiredAssets": [
                      {
                        "category": "TOPPING|DECORATION|SAUCE|SPRINKLES",
                        "type": "string",
                        "assetKey": "string",
                        "query": "string",
                        "color": "string|null",
                        "anchor": "top_center|top_left|top_right|top_back|top_ring|side_front|top_drizzle",
                        "relativeSize": "small|medium|large",
                        "materialTarget": "string"
                      }
                    ],
                    "modelPrompt": "string",
                    "missingPolicy": "IF_ASSET_NOT_FOUND_MARK_PENDING_MANUAL_MODEL"
                  }
                }

                Pedido del usuario:
                "%s"
                """.formatted(userPrompt == null ? "" : userPrompt.trim());
    }

    private String extractText(Map<String, Object> raw) {
        Object candidatesObj = raw.get("candidates");

        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }

        Object firstObj = candidates.get(0);

        if (!(firstObj instanceof Map<?, ?> first)) {
            return null;
        }

        Object contentObj = first.get("content");

        if (!(contentObj instanceof Map<?, ?> content)) {
            return null;
        }

        Object partsObj = content.get("parts");

        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return null;
        }

        Object partObj = parts.get(0);

        if (!(partObj instanceof Map<?, ?> part)) {
            return null;
        }

        Object text = part.get("text");
        return text == null ? null : text.toString();
    }

    private String cleanJsonText(String text) {
        String cleaned = text.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        }

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        return cleaned;
    }

    private Map<String, Object> fallbackPlan(String prompt) {
        String rawPrompt = prompt == null ? "" : prompt.trim();
        String lower = rawPrompt.toLowerCase(Locale.ROOT);

        String dessertType = resolveDessertType(lower);
        String color = resolveColor(lower);
        List<Map<String, Object>> requiredAssets = resolveRequiredAssets(lower, color);

        Map<String, Object> recipe = buildFallbackRecipe(dessertType, requiredAssets);
        Map<String, Object> visualPlan = buildFallbackVisualPlan(rawPrompt, lower, dessertType, requiredAssets);

        Map<String, Object> result = new HashMap<>();
        result.put("recipe", recipe);
        result.put("visualPlan", visualPlan);
        return result;
    }

    private Map<String, Object> buildFallbackRecipe(String dessertType, List<Map<String, Object>> requiredAssets) {
        String title = buildTitle(dessertType, requiredAssets);

        Map<String, Object> recipe = new LinkedHashMap<>();
        recipe.put("title", title);
        recipe.put("dessertType", dessertType);
        recipe.put("description", buildDescription(dessertType, requiredAssets));
        recipe.put("servings", defaultServings(dessertType));
        recipe.put("difficulty", defaultDifficulty(dessertType));
        recipe.put("estimatedMinutes", defaultMinutes(dessertType));
        recipe.put("ingredients", fallbackIngredients(dessertType, requiredAssets));
        recipe.put("steps", fallbackSteps(dessertType, requiredAssets));
        recipe.put("learningTips", fallbackLearningTips(dessertType));

        return recipe;
    }

    private Map<String, Object> buildFallbackVisualPlan(
            String rawPrompt,
            String lower,
            String dessertType,
            List<Map<String, Object>> requiredAssets
    ) {
        Map<String, Object> visualPlan = new LinkedHashMap<>();
        visualPlan.put("strategy", "COMPOSE_SCENE");
        visualPlan.put("normalizedPrompt", lower);
        visualPlan.put("dessertType", dessertType);
        visualPlan.put("keywords", buildKeywords(dessertType, requiredAssets));
        visualPlan.put("baseAssetQuery", dessertType + " base");
        visualPlan.put("requiredAssets", requiredAssets);
        visualPlan.put("modelPrompt", buildModelPrompt(rawPrompt, dessertType, requiredAssets));
        visualPlan.put("missingPolicy", "IF_ASSET_NOT_FOUND_MARK_PENDING_MANUAL_MODEL");
        return visualPlan;
    }

    private String resolveDessertType(String lower) {
        if (containsAny(lower, "milhojas", "mille feuille", "mille-feuille", "millefeuille")) {
            return "milhojas";
        }

        if (containsAny(lower, "tres leches", "tres_leches")) {
            return "tres_leches";
        }

        if (containsAny(lower, "cheesecake", "pay de queso", "tarta de queso")) {
            return "cheesecake";
        }

        if (containsAny(lower, "tarta", "tart", "pie", "pay")) {
            return "tart";
        }

        if (containsAny(lower, "brownie")) {
            return "brownie";
        }

        if (containsAny(lower, "macaron", "macarrón", "macarron")) {
            return "macaron";
        }

        if (containsAny(lower, "tiramisú", "tiramisu")) {
            return "tiramisu";
        }

        if (containsAny(lower, "flan")) {
            return "flan";
        }

        if (containsAny(lower, "alfajor", "alfajores")) {
            return "alfajor";
        }

        if (containsAny(lower, "cupcake", "magdalena")) {
            return "cupcake";
        }

        if (containsAny(lower, "donut", "dona", "rosquilla")) {
            return "donut";
        }

        if (containsAny(lower, "galleta", "cookie")) {
            return "cookie";
        }

        if (containsAny(lower, "pastel", "cake", "torta", "bizcocho", "ponqué", "ponque")) {
            return "round_cake";
        }

        return "generic_dessert";
    }

    private List<Map<String, Object>> resolveRequiredAssets(String lower, String color) {
        List<Map<String, Object>> assets = new ArrayList<>();

        if (containsAny(lower, "rosa", "rose", "flor")) {
            assets.add(requiredAsset(
                    "TOPPING",
                    "rose",
                    "caramel_rose",
                    "caramel rose decoration",
                    color,
                    "top_center",
                    "small",
                    "petals"
            ));
        }

        if (containsAny(lower, "fresa", "fresas", "strawberry", "frutilla", "frutillas")) {
            assets.add(requiredAsset(
                    "TOPPING",
                    "strawberry",
                    "strawberry_topping",
                    "strawberry topping",
                    "#D62828",
                    "top_ring",
                    "small",
                    "primary"
            ));
        }

        if (containsAny(lower, "vela", "velas", "candle", "cumpleaños", "cumpleanos")) {
            assets.add(requiredAsset(
                    "DECORATION",
                    "candle",
                    "birthday_candle",
                    "birthday candle",
                    resolveColorOrDefault(lower, "#FFD166"),
                    "top_back",
                    "medium",
                    "primary"
            ));
        }

        if (containsAny(lower, "chocolate", "salsa de chocolate", "baño de chocolate", "bano de chocolate")) {
            assets.add(requiredAsset(
                    "SAUCE",
                    "chocolate",
                    "chocolate_drizzle",
                    "chocolate drizzle sauce",
                    "#4B2418",
                    "top_drizzle",
                    "medium",
                    "chocolate"
            ));
        }

        if (containsAny(lower, "crema", "nata", "cream", "copete")) {
            assets.add(requiredAsset(
                    "TOPPING",
                    "cream",
                    "cream_swirl",
                    "cream swirl topping",
                    "#FFF2E6",
                    "top_center",
                    "medium",
                    "cream"
            ));
        }

        if (containsAny(lower, "chispas", "sprinkles", "grageas")) {
            assets.add(requiredAsset(
                    "SPRINKLES",
                    "sprinkles",
                    "rainbow_sprinkles",
                    "rainbow sprinkles",
                    null,
                    "top_ring",
                    "small",
                    "primary"
            ));
        }

        return assets;
    }

    private Map<String, Object> requiredAsset(
            String category,
            String type,
            String assetKey,
            String query,
            String color,
            String anchor,
            String relativeSize,
            String materialTarget
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("category", category);
        item.put("type", type);
        item.put("assetKey", assetKey);
        item.put("query", query);
        item.put("color", color);
        item.put("anchor", anchor);
        item.put("relativeSize", relativeSize);
        item.put("materialTarget", materialTarget);
        return item;
    }

    private List<Map<String, Object>> fallbackIngredients(String dessertType, List<Map<String, Object>> requiredAssets) {
        List<Map<String, Object>> ingredients = new ArrayList<>();

        switch (dessertType) {
            case "milhojas" -> {
                ingredients.add(ingredient("Láminas de hojaldre", "3", "unidades"));
                ingredients.add(ingredient("Leche entera", "500", "ml"));
                ingredients.add(ingredient("Yemas de huevo", "4", "unidades"));
                ingredients.add(ingredient("Azúcar", "120", "g"));
                ingredients.add(ingredient("Maicena", "40", "g"));
                ingredients.add(ingredient("Esencia de vainilla", "1", "cdta"));
                ingredients.add(ingredient("Azúcar pulverizada", "30", "g"));
            }
            case "cheesecake" -> {
                ingredients.add(ingredient("Galletas trituradas", "180", "g"));
                ingredients.add(ingredient("Mantequilla derretida", "80", "g"));
                ingredients.add(ingredient("Queso crema", "450", "g"));
                ingredients.add(ingredient("Azúcar", "140", "g"));
                ingredients.add(ingredient("Huevos", "3", "unidades"));
                ingredients.add(ingredient("Crema de leche", "120", "ml"));
            }
            case "cupcake" -> {
                ingredients.add(ingredient("Harina de trigo", "180", "g"));
                ingredients.add(ingredient("Azúcar", "140", "g"));
                ingredients.add(ingredient("Huevos", "2", "unidades"));
                ingredients.add(ingredient("Mantequilla", "90", "g"));
                ingredients.add(ingredient("Leche", "100", "ml"));
                ingredients.add(ingredient("Polvo de hornear", "1", "cdta"));
            }
            case "brownie" -> {
                ingredients.add(ingredient("Chocolate semiamargo", "180", "g"));
                ingredients.add(ingredient("Mantequilla", "120", "g"));
                ingredients.add(ingredient("Azúcar", "180", "g"));
                ingredients.add(ingredient("Huevos", "3", "unidades"));
                ingredients.add(ingredient("Harina", "90", "g"));
                ingredients.add(ingredient("Cacao en polvo", "25", "g"));
            }
            default -> {
                ingredients.add(ingredient("Harina de trigo", "250", "g"));
                ingredients.add(ingredient("Azúcar", "180", "g"));
                ingredients.add(ingredient("Huevos", "3", "unidades"));
                ingredients.add(ingredient("Mantequilla", "120", "g"));
                ingredients.add(ingredient("Leche", "160", "ml"));
                ingredients.add(ingredient("Polvo de hornear", "1", "cdta"));
                ingredients.add(ingredient("Esencia de vainilla", "1", "cdta"));
            }
        }

        if (containsAssetType(requiredAssets, "rose")) {
            ingredients.add(ingredient("Caramelo moldeable o fondant para rosa", "80", "g"));
        }

        if (containsAssetType(requiredAssets, "strawberry")) {
            ingredients.add(ingredient("Fresas frescas", "120", "g"));
        }

        if (containsAssetType(requiredAssets, "chocolate")) {
            ingredients.add(ingredient("Chocolate para cobertura", "100", "g"));
        }

        if (containsAssetType(requiredAssets, "cream")) {
            ingredients.add(ingredient("Crema para batir", "150", "ml"));
        }

        return ingredients;
    }

    private List<Map<String, Object>> fallbackSteps(String dessertType, List<Map<String, Object>> requiredAssets) {
        List<Map<String, Object>> steps = new ArrayList<>();

        if ("milhojas".equals(dessertType)) {
            steps.add(step(1, "Hornear el hojaldre", "Cortar las láminas de hojaldre al tamaño deseado, pincharlas con un tenedor y hornear hasta que estén doradas y crujientes."));
            steps.add(step(2, "Preparar la crema pastelera", "Calentar la leche con vainilla. Aparte, mezclar yemas, azúcar y maicena. Integrar la leche caliente y cocinar hasta espesar."));
            steps.add(step(3, "Enfriar la crema", "Cubrir la crema con plástico en contacto y dejar enfriar para que mantenga buena textura al montar."));
            steps.add(step(4, "Montar las capas", "Alternar capas de hojaldre con crema pastelera, presionando suavemente para mantener estabilidad."));
        } else {
            steps.add(step(1, "Preparar la base", "Mezclar los ingredientes secos y luego integrar los ingredientes húmedos hasta obtener una mezcla homogénea."));
            steps.add(step(2, "Hornear", "Verter la preparación en el molde adecuado y hornear hasta que el centro esté firme."));
            steps.add(step(3, "Enfriar", "Dejar enfriar completamente antes de decorar para evitar que la cobertura se derrita o pierda forma."));
        }

        if (containsAssetType(requiredAssets, "rose")) {
            steps.add(step(steps.size() + 1, "Crear la rosa decorativa", "Formar pétalos delgados con caramelo moldeable o fondant y ensamblarlos desde el centro hacia afuera."));
        }

        if (containsAssetType(requiredAssets, "strawberry")) {
            steps.add(step(steps.size() + 1, "Preparar las fresas", "Lavar, secar y cortar las fresas para usarlas como decoración superior."));
        }

        if (containsAssetType(requiredAssets, "chocolate")) {
            steps.add(step(steps.size() + 1, "Agregar chocolate", "Fundir el chocolate y aplicarlo como líneas o baño ligero sobre la superficie."));
        }

        if (containsAssetType(requiredAssets, "cream")) {
            steps.add(step(steps.size() + 1, "Aplicar crema", "Batir la crema hasta formar picos firmes y decorar con manga pastelera."));
        }

        steps.add(step(steps.size() + 1, "Presentar", "Ubicar las decoraciones según el diseño visual y servir cuando la estructura esté estable."));

        return steps;
    }

    private List<String> fallbackLearningTips(String dessertType) {
        if ("milhojas".equals(dessertType)) {
            return List.of(
                    "El hojaldre debe estar bien horneado para que no pierda textura con la crema.",
                    "La crema pastelera debe enfriarse antes del montaje.",
                    "Una decoración pequeña encima evita que la milhojas pierda proporción visual."
            );
        }

        return List.of(
                "Dejar enfriar la base antes de decorar mejora el acabado.",
                "La proporción entre base y topping ayuda a que el postre se vea equilibrado.",
                "Los colores de decoración deben contrastar con la base para destacar visualmente."
        );
    }

    private Map<String, Object> ingredient(String name, String quantity, String unit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("quantity", quantity);
        item.put("unit", unit);
        return item;
    }

    private Map<String, Object> step(int order, String title, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("order", order);
        item.put("title", title);
        item.put("description", description);
        return item;
    }

    private String buildTitle(String dessertType, List<Map<String, Object>> requiredAssets) {
        String base = switch (dessertType) {
            case "milhojas" -> "Milhojas clásica";
            case "cupcake" -> "Cupcake artesanal";
            case "cheesecake" -> "Cheesecake cremoso";
            case "tart" -> "Tarta artesanal";
            case "brownie" -> "Brownie intenso de chocolate";
            case "cookie" -> "Galleta decorada";
            case "donut" -> "Dona decorada";
            case "macaron" -> "Macaron francés";
            case "tiramisu" -> "Tiramisú clásico";
            case "flan" -> "Flan suave";
            case "alfajor" -> "Alfajor artesanal";
            case "tres_leches" -> "Pastel tres leches";
            case "round_cake" -> "Pastel redondo";
            default -> "Postre personalizado";
        };

        if (containsAssetType(requiredAssets, "rose")) {
            return base + " con rosa decorativa";
        }

        if (containsAssetType(requiredAssets, "strawberry")) {
            return base + " con fresas";
        }

        if (containsAssetType(requiredAssets, "chocolate")) {
            return base + " con chocolate";
        }

        return base;
    }

    private String buildDescription(String dessertType, List<Map<String, Object>> requiredAssets) {
        String base = switch (dessertType) {
            case "milhojas" -> "Postre de hojaldre crujiente con capas de crema pastelera, pensado para una presentación elegante.";
            case "cheesecake" -> "Postre cremoso con base de galleta y textura suave, ideal para decorar con elementos superiores.";
            case "brownie" -> "Postre de chocolate denso y húmedo, con superficie firme para decoraciones pequeñas.";
            default -> "Postre personalizado generado para aprendizaje de repostería y visualización 3D.";
        };

        if (!requiredAssets.isEmpty()) {
            return base + " Incluye decoración personalizada según el pedido del usuario.";
        }

        return base;
    }

    private Integer defaultServings(String dessertType) {
        return switch (dessertType) {
            case "cupcake", "macaron", "donut", "alfajor" -> 6;
            case "milhojas", "brownie", "tart" -> 8;
            default -> 10;
        };
    }

    private String defaultDifficulty(String dessertType) {
        return switch (dessertType) {
            case "milhojas", "macaron", "tiramisu" -> "ADVANCED";
            case "cheesecake", "tart", "brownie" -> "INTERMEDIATE";
            default -> "BEGINNER";
        };
    }

    private Integer defaultMinutes(String dessertType) {
        return switch (dessertType) {
            case "milhojas" -> 120;
            case "cheesecake" -> 150;
            case "macaron" -> 140;
            case "tiramisu" -> 180;
            case "brownie" -> 60;
            default -> 90;
        };
    }

    private String buildModelPrompt(String rawPrompt, String dessertType, List<Map<String, Object>> requiredAssets) {
        StringBuilder builder = new StringBuilder();
        builder.append("Clean centered ")
                .append(dessertType.replace("_", " "))
                .append(" dessert asset composition");

        if (!requiredAssets.isEmpty()) {
            builder.append(" with ");
            List<String> names = requiredAssets.stream()
                    .map(asset -> String.valueOf(asset.getOrDefault("type", "decoration")))
                    .toList();
            builder.append(String.join(", ", names));
        }

        builder.append(". GLB friendly, no plate, no background. User request: ");
        builder.append(rawPrompt == null ? "" : rawPrompt);

        return builder.toString();
    }

    private List<String> buildKeywords(String dessertType, List<Map<String, Object>> requiredAssets) {
        List<String> keywords = new ArrayList<>();
        keywords.add("dessert");
        keywords.add(dessertType);

        for (Map<String, Object> asset : requiredAssets) {
            Object type = asset.get("type");
            Object assetKey = asset.get("assetKey");

            if (type != null) {
                keywords.add(type.toString());
            }

            if (assetKey != null) {
                keywords.add(assetKey.toString());
            }
        }

        return keywords;
    }

    private String resolveColor(String lower) {
        if (containsAny(lower, "azul", "blue")) {
            return "#2F80ED";
        }

        if (containsAny(lower, "roja", "rojo", "red")) {
            return "#C92A3A";
        }

        if (containsAny(lower, "rosada", "rosado", "pink", "color rosa")) {
            return "#FFD6E7";
        }

        if (containsAny(lower, "amarilla", "amarillo", "yellow")) {
            return "#FFD166";
        }

        if (containsAny(lower, "verde", "green")) {
            return "#2A7F62";
        }

        if (containsAny(lower, "morada", "morado", "purple", "violeta")) {
            return "#7B2CBF";
        }

        if (containsAny(lower, "blanca", "blanco", "white")) {
            return "#FFFFFF";
        }

        if (containsAny(lower, "negra", "negro", "black")) {
            return "#1F1F1F";
        }

        if (containsAny(lower, "chocolate", "café", "cafe", "brown")) {
            return "#4B2418";
        }

        return "#C92A3A";
    }

    private String resolveColorOrDefault(String lower, String fallback) {
        String color = resolveColor(lower);
        return color == null || color.isBlank() ? fallback : color;
    }

    private boolean containsAssetType(List<Map<String, Object>> assets, String type) {
        return assets.stream()
                .anyMatch(asset -> type.equals(String.valueOf(asset.get("type"))));
    }

    private boolean containsAny(String text, String... values) {
        if (text == null || text.isBlank()) {
            return false;
        }

        for (String value : values) {
            if (text.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }
}