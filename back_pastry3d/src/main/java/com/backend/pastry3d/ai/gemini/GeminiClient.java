package com.backend.pastry3d.ai.gemini;

import com.backend.pastry3d.shared.exception.BadRequestException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            ObjectMapper objectMapper,
            @Value("${gemini.enabled:false}") boolean enabled,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-1.5-flash}") String model
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public Map<String, Object> generateRecipePlan(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new BadRequestException("El prompt no puede estar vacío");
        }

        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return fallbackPlan(userPrompt);
        }

        try {
            String instruction = buildInstruction(userPrompt);
            String body = buildGeminiRequest(instruction);

            String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + encodedModel + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallbackPlan(userPrompt);
            }

            String text = extractGeminiText(response.body());
            String json = cleanJson(text);

            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});
            return normalizePlan(result, userPrompt);
        } catch (Exception exception) {
            return fallbackPlan(userPrompt);
        }
    }

    private String buildInstruction(String userPrompt) {
        return """
                Eres el motor culinario y visual de Pastry3D.
                Debes generar una receta realista en español y un plan visual 3D compatible con una biblioteca de modelos GLB.
                
                Pedido del usuario:
                "%s"
                
                Reglas críticas:
                1. Respeta el postre principal pedido por el usuario. No cambies torta por flan, ni flan por pastel, ni dona por helado.
                2. La receta debe parecer real: ingredientes coherentes, cantidades, tiempos y pasos prácticos.
                3. No repitas siempre la misma receta base. Varía según el postre: flan, dona, helado, milhojas, cheesecake, cupcake, tarta o pastel.
                4. Separa lo culinario de lo visual. Una rosa o una cereza puede ser decoración visual, no necesariamente ingrediente principal.
                5. Si existe un modelo completo vistoso, úsalo con requiredAssets vacío.
                6. Si el usuario pide una base combinable con toppings, usa requiredAssets para pedir toppings.
                7. Si pide dos toppings, incluye ambos y usa anchors diferentes.
                8. Si pide algo que no existe, por ejemplo dragón, corona, unicornio o personaje, inclúyelo como requiredAsset con un assetKey descriptivo para que el backend lo marque como faltante.
                9. Devuelve solo JSON válido. No uses markdown.
                
                Tipos de postre disponibles:
                - flan: flan napolitano completo
                - chocolate_cake: pastel o torta de chocolate completo
                - strawberry_cake: pastel o torta de fresa completo
                - donut: dona glaseada completa
                - ice_cream: copa de helado completa
                - round_cake: pastel/torta redonda combinable
                - milhojas: milhojas combinable
                - cupcake: cupcake combinable
                - cheesecake: cheesecake combinable
                - tart: tarta combinable
                
                Assets visuales disponibles:
                Bases:
                - round_cake_base_vanilla
                - milhojas_base_classic
                - cupcake_base_vanilla
                - cheesecake_base_plain
                - tart_base_vanilla
                
                Modelos completos:
                - flan_napolitano_complete, dessertType flan
                - chocolate_cake_complete, dessertType chocolate_cake
                - strawberry_cake_complete, dessertType strawberry_cake
                - donut_glazed_complete, dessertType donut
                - ice_cream_cup_complete, dessertType ice_cream
                
                Toppings:
                - caramel_rose, rosa o rosas de caramelo
                - strawberry_topping, fresa
                - blueberry_topping, arándanos
                - cherry_topping, cereza
                - cream_swirl, crema o chantilly
                - birthday_candle, vela
                - macaron_topping, macaron
                - chocolate_bar_piece, trozo de chocolate
                - chocolate_drizzle, salsa o chorreado de chocolate
                
                Anchors permitidos:
                - top_center
                - top_left
                - top_right
                - top_front
                - top_back
                - side_front
                - top_drizzle
                
                Colores sugeridos:
                - rosa rojo: #C92A3A
                - rosa azul: #2F80ED
                - fresa rojo: #D72638
                - cereza rojo: #C9182B
                - arándano azul: #2F3E8C
                - chocolate: #4B2418
                - crema: #FFF2E6
                - dorado: #D4AF37
                
                Formato obligatorio:
                {
                  "recipe": {
                    "title": "string",
                    "dessertType": "string",
                    "difficulty": "BEGINNER|INTERMEDIATE|ADVANCED",
                    "servings": 8,
                    "estimatedMinutes": 60,
                    "description": "string",
                    "ingredients": [
                      {
                        "name": "string",
                        "quantity": "string",
                        "unit": "string"
                      }
                    ],
                    "steps": [
                      {
                        "title": "string",
                        "description": "string"
                      }
                    ]
                  },
                  "visualPlan": {
                    "dessertType": "string",
                    "normalizedPrompt": "string",
                    "modelPrompt": "English prompt for generating a clean centered 3D GLB asset if a missing model is needed",
                    "requiredAssets": [
                      {
                        "assetKey": "string",
                        "category": "TOPPING|SAUCE|DECORATION",
                        "type": "string",
                        "query": "string",
                        "anchor": "top_center",
                        "relativeSize": "small|medium|large",
                        "color": "#HEX",
                        "materialTarget": "primary"
                      }
                    ]
                  }
                }
                
                Ejemplos de decisión:
                - "flan napolitano" => dessertType flan, requiredAssets []
                - "dona glaseada" => dessertType donut, requiredAssets []
                - "copa de helado" => dessertType ice_cream, requiredAssets []
                - "pastel de chocolate" => dessertType chocolate_cake, requiredAssets []
                - "pastel de fresa" => dessertType strawberry_cake, requiredAssets []
                - "torta con rosas y fresas" => dessertType round_cake, requiredAssets caramel_rose y strawberry_topping
                - "milhojas con cerezas y arándanos" => dessertType milhojas, requiredAssets cherry_topping y blueberry_topping
                - "pastel con dragón dorado" => dessertType round_cake, requiredAssets dragon_gold_topper
                """.formatted(userPrompt);
    }

    private String buildGeminiRequest(String instruction) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();

            Map<String, Object> part = Map.of("text", instruction);
            Map<String, Object> content = Map.of("parts", List.of(part));

            payload.put("contents", List.of(content));
            payload.put("generationConfig", Map.of(
                    "temperature", 0.85,
                    "topP", 0.9,
                    "topK", 40,
                    "maxOutputTokens", 4096,
                    "responseMimeType", "application/json"
            ));

            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo construir la solicitud a Gemini");
        }
    }

    private String extractGeminiText(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {});
            Object candidatesObject = response.get("candidates");

            if (!(candidatesObject instanceof List<?> candidates) || candidates.isEmpty()) {
                throw new BadRequestException("Gemini no devolvió candidatos");
            }

            Object firstCandidate = candidates.get(0);

            if (!(firstCandidate instanceof Map<?, ?> candidateMap)) {
                throw new BadRequestException("Respuesta inválida de Gemini");
            }

            Object contentObject = candidateMap.get("content");

            if (!(contentObject instanceof Map<?, ?> contentMap)) {
                throw new BadRequestException("Gemini no devolvió contenido");
            }

            Object partsObject = contentMap.get("parts");

            if (!(partsObject instanceof List<?> parts) || parts.isEmpty()) {
                throw new BadRequestException("Gemini no devolvió partes");
            }

            Object firstPart = parts.get(0);

            if (!(firstPart instanceof Map<?, ?> partMap)) {
                throw new BadRequestException("Parte inválida de Gemini");
            }

            Object text = partMap.get("text");

            if (text == null || text.toString().isBlank()) {
                throw new BadRequestException("Gemini devolvió texto vacío");
            }

            return text.toString();
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo leer la respuesta de Gemini");
        }
    }

    private String cleanJson(String text) {
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

    private Map<String, Object> normalizePlan(Map<String, Object> result, String userPrompt) {
        Map<String, Object> recipe = mapValue(result.get("recipe"));
        Map<String, Object> visualPlan = mapValue(result.get("visualPlan"));

        String dessertType = normalizeDessertType(
                stringValue(recipe.get("dessertType"), stringValue(visualPlan.get("dessertType"), detectDessertType(userPrompt)))
        );

        recipe.put("dessertType", dessertType);
        recipe.putIfAbsent("title", titleForDessert(dessertType, userPrompt));
        recipe.putIfAbsent("difficulty", "BEGINNER");
        recipe.putIfAbsent("servings", 8);
        recipe.putIfAbsent("estimatedMinutes", estimatedMinutes(dessertType));
        recipe.putIfAbsent("description", descriptionForDessert(dessertType));

        if (!(recipe.get("ingredients") instanceof List<?>)) {
            recipe.put("ingredients", fallbackIngredients(dessertType));
        }

        if (!(recipe.get("steps") instanceof List<?>)) {
            recipe.put("steps", fallbackSteps(dessertType));
        }

        visualPlan.put("dessertType", dessertType);
        visualPlan.putIfAbsent("normalizedPrompt", userPrompt);
        visualPlan.putIfAbsent("modelPrompt", buildModelPrompt(userPrompt, dessertType));

        if (!(visualPlan.get("requiredAssets") instanceof List<?>)) {
            visualPlan.put("requiredAssets", fallbackRequiredAssets(userPrompt, dessertType));
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("recipe", recipe);
        normalized.put("visualPlan", visualPlan);
        return normalized;
    }

    private Map<String, Object> fallbackPlan(String userPrompt) {
        String dessertType = detectDessertType(userPrompt);

        Map<String, Object> recipe = new LinkedHashMap<>();
        recipe.put("title", titleForDessert(dessertType, userPrompt));
        recipe.put("dessertType", dessertType);
        recipe.put("difficulty", "BEGINNER");
        recipe.put("servings", 8);
        recipe.put("estimatedMinutes", estimatedMinutes(dessertType));
        recipe.put("description", descriptionForDessert(dessertType));
        recipe.put("ingredients", fallbackIngredients(dessertType));
        recipe.put("steps", fallbackSteps(dessertType));

        Map<String, Object> visualPlan = new LinkedHashMap<>();
        visualPlan.put("dessertType", dessertType);
        visualPlan.put("normalizedPrompt", userPrompt);
        visualPlan.put("modelPrompt", buildModelPrompt(userPrompt, dessertType));
        visualPlan.put("requiredAssets", fallbackRequiredAssets(userPrompt, dessertType));

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("recipe", recipe);
        plan.put("visualPlan", visualPlan);
        return plan;
    }

    private String detectDessertType(String prompt) {
        String text = prompt == null ? "" : prompt.toLowerCase();

        if (containsAny(text, "flan", "napolitano")) {
            return "flan";
        }

        if (containsAny(text, "dona", "donut", "rosquilla")) {
            return "donut";
        }

        if (containsAny(text, "helado", "ice cream", "copa de helado")) {
            return "ice_cream";
        }

        if (containsAny(text, "milhojas", "mille feuille", "millefeuille")) {
            return "milhojas";
        }

        if (containsAny(text, "cupcake", "magdalena")) {
            return "cupcake";
        }

        if (containsAny(text, "cheesecake", "tarta de queso", "pay de queso")) {
            return "cheesecake";
        }

        if (containsAny(text, "tarta", "pay", "pie")) {
            return "tart";
        }

        if (containsAny(text, "fresa", "strawberry") && containsAny(text, "pastel", "torta", "cake")) {
            return "strawberry_cake";
        }

        if (containsAny(text, "chocolate", "cacao") && containsAny(text, "pastel", "torta", "cake")) {
            return "chocolate_cake";
        }

        return "round_cake";
    }

    private String normalizeDessertType(String dessertType) {
        if (dessertType == null || dessertType.isBlank()) {
            return "round_cake";
        }

        String value = dessertType.trim().toLowerCase()
                .replace("-", "_")
                .replace(" ", "_");

        return switch (value) {
            case "cake", "pastel", "torta", "roundcake", "round_cake", "vanilla_cake" -> "round_cake";
            case "chocolate", "chocolatecake", "chocolate_cake", "pastel_de_chocolate", "torta_de_chocolate" -> "chocolate_cake";
            case "strawberry", "strawberrycake", "strawberry_cake", "pastel_de_fresa", "torta_de_fresa" -> "strawberry_cake";
            case "icecream", "ice_cream", "helado", "copa_de_helado" -> "ice_cream";
            case "donut", "dona", "rosquilla" -> "donut";
            case "flan", "flan_napolitano" -> "flan";
            case "milhojas", "millefeuille", "mille_feuille" -> "milhojas";
            case "cupcake" -> "cupcake";
            case "cheesecake" -> "cheesecake";
            case "tart", "tarta", "pie", "pay" -> "tart";
            default -> value;
        };
    }

    private List<Map<String, Object>> fallbackRequiredAssets(String prompt, String dessertType) {
        String text = prompt == null ? "" : prompt.toLowerCase();
        List<Map<String, Object>> assets = new ArrayList<>();

        if (isCompleteShowcaseDessert(dessertType) && !containsAny(text, "con rosa", "con fres", "con cereza", "con arándano", "con arandano", "con vela", "con crema")) {
            return assets;
        }

        if (containsAny(text, "rosa", "rosas", "flor", "flores")) {
            assets.add(asset("caramel_rose", "TOPPING", "rose", "rosas de caramelo", "top_center", "medium", "#C92A3A", "petals"));
        }

        if (containsAny(text, "fresa", "fresas", "strawberry")) {
            assets.add(asset("strawberry_topping", "TOPPING", "strawberry", "fresas frescas", nextAnchor(assets.size()), "small", "#D72638", "primary"));
        }

        if (containsAny(text, "arándano", "arandano", "arándanos", "arandanos", "blueberry")) {
            assets.add(asset("blueberry_topping", "TOPPING", "blueberry", "arándanos decorativos", nextAnchor(assets.size()), "small", "#2F3E8C", "primary"));
        }

        if (containsAny(text, "cereza", "cerezas", "cherry")) {
            assets.add(asset("cherry_topping", "TOPPING", "cherry", "cerezas decorativas", nextAnchor(assets.size()), "small", "#C9182B", "primary"));
        }

        if (containsAny(text, "crema", "chantilly", "nata")) {
            assets.add(asset("cream_swirl", "TOPPING", "cream", "remolino de crema", nextAnchor(assets.size()), "small", "#FFF2E6", "cream"));
        }

        if (containsAny(text, "vela", "cumpleaños", "cumpleanos", "birthday")) {
            assets.add(asset("birthday_candle", "DECORATION", "candle", "vela de cumpleaños", nextAnchor(assets.size()), "small", "#F2C94C", "primary"));
        }

        if (containsAny(text, "macaron", "macarron", "macarrón")) {
            assets.add(asset("macaron_topping", "TOPPING", "macaron", "macaron decorativo", nextAnchor(assets.size()), "small", "#FFD6E7", "primary"));
        }

        if (containsAny(text, "dragón", "dragon")) {
            assets.add(asset("golden_dragon_topper", "DECORATION", "dragon", "golden dragon cake topper", "top_center", "medium", "#D4AF37", "primary"));
        }

        return assets;
    }

    private Map<String, Object> asset(String assetKey, String category, String type, String query, String anchor, String relativeSize, String color, String materialTarget) {
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("assetKey", assetKey);
        asset.put("category", category);
        asset.put("type", type);
        asset.put("query", query);
        asset.put("anchor", anchor);
        asset.put("relativeSize", relativeSize);
        asset.put("color", color);
        asset.put("materialTarget", materialTarget);
        return asset;
    }

    private String nextAnchor(int index) {
        return switch (index) {
            case 0 -> "top_center";
            case 1 -> "top_left";
            case 2 -> "top_right";
            case 3 -> "top_front";
            default -> "top_back";
        };
    }

    private boolean isCompleteShowcaseDessert(String dessertType) {
        return List.of("flan", "chocolate_cake", "strawberry_cake", "donut", "ice_cream").contains(dessertType);
    }

    private String titleForDessert(String dessertType, String prompt) {
        return switch (dessertType) {
            case "flan" -> "Flan napolitano clásico";
            case "chocolate_cake" -> "Pastel de chocolate intenso";
            case "strawberry_cake" -> "Pastel de fresa decorado";
            case "donut" -> "Dona glaseada artesanal";
            case "ice_cream" -> "Copa de helado cremosa";
            case "milhojas" -> "Milhojas clásica de crema";
            case "cupcake" -> "Cupcake decorado de vainilla";
            case "cheesecake" -> "Cheesecake suave tradicional";
            case "tart" -> "Tarta dulce de vainilla";
            default -> "Pastel redondo personalizado";
        };
    }

    private String descriptionForDessert(String dessertType) {
        return switch (dessertType) {
            case "flan" -> "Postre cremoso de huevo, leche y caramelo, con textura suave y sabor tradicional.";
            case "chocolate_cake" -> "Pastel húmedo de chocolate con sabor profundo a cacao y presentación elegante.";
            case "strawberry_cake" -> "Pastel suave con notas de fresa, ideal para una presentación fresca y llamativa.";
            case "donut" -> "Dona esponjosa con glaseado brillante, pensada para una preparación sencilla y vistosa.";
            case "ice_cream" -> "Copa fría y cremosa con textura suave, ideal como postre rápido y visualmente atractivo.";
            case "milhojas" -> "Capas crujientes de hojaldre con crema suave, equilibrando textura y dulzor.";
            default -> "Postre personalizado generado a partir del pedido del usuario.";
        };
    }

    private int estimatedMinutes(String dessertType) {
        return switch (dessertType) {
            case "flan" -> 90;
            case "chocolate_cake", "strawberry_cake" -> 75;
            case "donut" -> 120;
            case "ice_cream" -> 240;
            case "milhojas" -> 80;
            case "cupcake" -> 45;
            case "cheesecake" -> 120;
            default -> 60;
        };
    }

    private List<Map<String, Object>> fallbackIngredients(String dessertType) {
        List<Map<String, Object>> ingredients = new ArrayList<>();

        switch (dessertType) {
            case "flan" -> {
                ingredients.add(ingredient("Huevos", "5", "unidades"));
                ingredients.add(ingredient("Leche condensada", "395", "g"));
                ingredients.add(ingredient("Leche evaporada", "360", "ml"));
                ingredients.add(ingredient("Queso crema", "180", "g"));
                ingredients.add(ingredient("Azúcar para caramelo", "120", "g"));
                ingredients.add(ingredient("Vainilla", "1", "cucharadita"));
            }
            case "chocolate_cake" -> {
                ingredients.add(ingredient("Harina de trigo", "220", "g"));
                ingredients.add(ingredient("Cacao en polvo", "60", "g"));
                ingredients.add(ingredient("Azúcar morena", "180", "g"));
                ingredients.add(ingredient("Huevos", "3", "unidades"));
                ingredients.add(ingredient("Leche", "180", "ml"));
                ingredients.add(ingredient("Mantequilla derretida", "120", "g"));
                ingredients.add(ingredient("Polvo de hornear", "10", "g"));
            }
            case "strawberry_cake" -> {
                ingredients.add(ingredient("Harina de trigo", "230", "g"));
                ingredients.add(ingredient("Fresas trituradas", "160", "g"));
                ingredients.add(ingredient("Azúcar", "170", "g"));
                ingredients.add(ingredient("Huevos", "3", "unidades"));
                ingredients.add(ingredient("Yogur natural", "120", "g"));
                ingredients.add(ingredient("Mantequilla", "110", "g"));
            }
            case "donut" -> {
                ingredients.add(ingredient("Harina de fuerza", "300", "g"));
                ingredients.add(ingredient("Levadura seca", "7", "g"));
                ingredients.add(ingredient("Leche tibia", "160", "ml"));
                ingredients.add(ingredient("Azúcar", "45", "g"));
                ingredients.add(ingredient("Mantequilla", "45", "g"));
                ingredients.add(ingredient("Huevo", "1", "unidad"));
            }
            case "ice_cream" -> {
                ingredients.add(ingredient("Crema de leche", "400", "ml"));
                ingredients.add(ingredient("Leche condensada", "250", "g"));
                ingredients.add(ingredient("Vainilla", "1", "cucharadita"));
                ingredients.add(ingredient("Fruta o salsa al gusto", "100", "g"));
            }
            case "milhojas" -> {
                ingredients.add(ingredient("Masa de hojaldre", "500", "g"));
                ingredients.add(ingredient("Leche", "500", "ml"));
                ingredients.add(ingredient("Yemas", "4", "unidades"));
                ingredients.add(ingredient("Azúcar", "120", "g"));
                ingredients.add(ingredient("Maicena", "40", "g"));
                ingredients.add(ingredient("Vainilla", "1", "cucharadita"));
            }
            default -> {
                ingredients.add(ingredient("Harina de trigo", "220", "g"));
                ingredients.add(ingredient("Azúcar", "160", "g"));
                ingredients.add(ingredient("Huevos", "3", "unidades"));
                ingredients.add(ingredient("Leche", "160", "ml"));
                ingredients.add(ingredient("Mantequilla", "110", "g"));
                ingredients.add(ingredient("Polvo de hornear", "10", "g"));
            }
        }

        return ingredients;
    }

    private List<Map<String, Object>> fallbackSteps(String dessertType) {
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step("Preparar la base", "Organizar ingredientes, precalentar el horno o preparar el molde según el tipo de postre."));
        steps.add(step("Mezclar", "Integrar los ingredientes secos y líquidos hasta obtener una mezcla homogénea y estable."));
        steps.add(step("Cocinar o enfriar", "Hornear, cocinar a baño María, freír o congelar según corresponda al postre."));
        steps.add(step("Reposar", "Dejar enfriar o estabilizar para mejorar textura, corte y presentación."));
        steps.add(step("Decorar", "Agregar toppings o acabados visuales justo antes de servir para conservar textura y volumen."));
        return steps;
    }

    private Map<String, Object> ingredient(String name, String quantity, String unit) {
        Map<String, Object> ingredient = new LinkedHashMap<>();
        ingredient.put("name", name);
        ingredient.put("quantity", quantity);
        ingredient.put("unit", unit);
        return ingredient;
    }

    private Map<String, Object> step(String title, String description) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("title", title);
        step.put("description", description);
        return step;
    }

    private String buildModelPrompt(String userPrompt, String dessertType) {
        return "clean centered 3D GLB asset of " + dessertType + " based on this request: " + userPrompt + ". No plate, no hands, no text, no background, single object, realistic pastry material.";
    }

    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return converted;
        }
        return new LinkedHashMap<>();
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String text = value.toString();

        if (text.isBlank()) {
            return fallback;
        }

        return text;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }
}