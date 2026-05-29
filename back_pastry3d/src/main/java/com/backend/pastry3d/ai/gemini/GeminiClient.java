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
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

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


    public void validatePastryPrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new BadRequestException("Describe un postre o producto de repostería para generar la receta.");
        }

        String normalized = normalizeForValidation(userPrompt);

        if (containsBlockedContent(normalized)) {
            throw new BadRequestException("Aquí solo generamos postres y repostería. No se aceptan pedidos obscenos, violentos, ofensivos o ajenos al laboratorio dulce.");
        }

        if (!enabled || apiKey == null || apiKey.isBlank()) {
            if (!looksLikeDessertRequest(normalized)) {
                throw new BadRequestException("Aquí solo consideramos postres, pasteles, toppings y productos de repostería. Describe un postre real para poder generarlo.");
            }
            return;
        }

        try {
            String instruction = buildValidationInstruction(userPrompt);
            String body = buildGeminiRequest(instruction);

            String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + encodedModel + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }

            String text = extractGeminiText(response.body());
            String json = cleanJson(text);
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});

            boolean accepted = booleanValue(result.get("accepted"), true);
            if (!accepted) {
                String reason = stringValue(
                        result.get("reason"),
                        "Aquí solo consideramos postres y repostería. Reformula el pedido como un postre real."
                );
                throw new BadRequestException(reason);
            }
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception ignored) {
            // Si falla la validación remota, se conserva la validación local para no romper la demo.
            if (!looksLikeDessertRequest(normalized)) {
                throw new BadRequestException("Aquí solo consideramos postres, pasteles, toppings y productos de repostería. Describe un postre real para poder generarlo.");
            }
        }
    }


    private String buildValidationInstruction(String userPrompt) {
        return """
                Eres el moderador culinario de Pastry3D.
                Evalúa si el pedido del usuario corresponde a un postre, pastel, tarta, cupcake, flan, helado, gelato, sorbete, dona, milhojas, cheesecake, tiramisú, mousse, galleta, brownie, macaron, parfait, postre frío, postre latino, postre italiano, postre francés, postre árabe, postre japonés, postre de panadería dulce o decoración comestible de repostería. Acepta nombres internacionales de postres aunque no estén en la biblioteca local de modelos. Si el usuario escribe mal un nombre de postre, corrígelo al postre real más cercano antes de decidir. Trabaja con un catálogo amplio y progresivo de postres, por ejemplo: gelato, sorbet, tiramisu, panna cotta, baklava, cannoli, profiteroles, eclair, pavlova, mochi, tres leches, arroz con leche, lemon pie, apple pie, red velvet, carrot cake, alfajor, natilla, churros, brownie, crepes, waffle, pancakes dulces, macaron, mousse, parfait, trifle, opera cake, black forest cake, banoffee pie, key lime pie, pecan pie, pumpkin pie, rice pudding, pudding, custard, flan, quesillo, creme brulee, cheesecake, donut, cupcake, cake pop, roll cake, cinnamon roll, churro sundae, banana split, sundae, milkshake dessert, churros con chocolate, buñuelos, mazamorra dulce y brevas con arequipe. Si el usuario escribe mal un nombre de postre, corrígelo al postre real más cercano antes de decidir. Por ejemplo: llelato, jelato o yelato deben entenderse como gelato; tiramisu mal escrito debe entenderse como tiramisu; pana cota debe entenderse como panna cotta; crem brule debe entenderse como creme brulee, por ejemplo cannoli, eclair, profiteroles, pavlova, mochi, tres leches, arroz con leche, lemon pie, apple pie, red velvet, carrot cake, churros, alfajor, natilla, panna cotta, creme brulee, baklava y flan.

                Rechaza si el texto:
                - no trata de postres o repostería,
                - contiene obscenidad, insultos, violencia, odio, sexualización, drogas, armas o datos personales,
                - pide texto ofensivo sobre el pastel,
                - pide generar personas, desnudez, partes sexuales, sangre, armas, política o marcas/logos.

                Acepta si el pedido es un postre con toppings normales, colores, frutas, crema, chocolate, rosas comestibles, velas o decoraciones no ofensivas.

                Pedido:
                "%s"

                Devuelve solo JSON válido:
                {
                  "accepted": true,
                  "reason": ""
                }

                Si rechazas, usa una razón breve en español, por ejemplo:
                "Aquí solo consideramos postres y repostería. Describe un postre real para poder generarlo."
                """.formatted(userPrompt);
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
                8. Si el postre principal pedido por el usuario NO existe exactamente en la lista de tipos disponibles, NO lo reemplaces por round_cake, ice_cream, flan ni otro modelo parecido. Mantén el postre real solicitado como dessertType usando un slug en inglés o español sin espacios, por ejemplo gelato, tiramisu, panna_cotta, baklava, creme_brulee. En ese caso usa requiredAssets vacío para que el backend marque que falta el modelo completo y pueda enviarlo a Tripo.
                9. Si lo que falta es una decoración sobre un postre existente, por ejemplo dragón, corona, unicornio o personaje, inclúyelo como requiredAsset con un assetKey descriptivo para que el backend lo marque como faltante.
                10. Devuelve solo JSON válido. No uses markdown.
                
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
                - "gelato" => dessertType gelato, requiredAssets []
                - "tiramisú" => dessertType tiramisu, requiredAssets []
                - "panna cotta" => dessertType panna_cotta, requiredAssets []
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

        if (containsAny(text, "gelato")) {
            return "gelato";
        }

        if (containsAny(text, "sorbete", "sorbet")) {
            return "sorbet";
        }

        if (containsAny(text, "tiramisu", "tiramisú")) {
            return "tiramisu";
        }

        if (containsAny(text, "panna cotta", "pannacotta")) {
            return "panna_cotta";
        }

        if (containsAny(text, "baklava")) {
            return "baklava";
        }

        if (containsAny(text, "creme brulee", "crème brûlée", "crema catalana")) {
            return "creme_brulee";
        }

        if (containsAny(text, "cannoli", "cannolo")) {
            return "cannoli";
        }

        if (containsAny(text, "profiterol", "profiteroles")) {
            return "profiteroles";
        }

        if (containsAny(text, "eclair", "éclair")) {
            return "eclair";
        }

        if (containsAny(text, "pavlova")) {
            return "pavlova";
        }

        if (containsAny(text, "mochi")) {
            return "mochi";
        }

        if (containsAny(text, "tres leches", "torta tres leches", "pastel tres leches")) {
            return "tres_leches";
        }

        if (containsAny(text, "arroz con leche")) {
            return "arroz_con_leche";
        }

        if (containsAny(text, "lemon pie", "pie de limon", "pie de limón", "tarta de limon", "tarta de limón")) {
            return "lemon_pie";
        }

        if (containsAny(text, "apple pie", "pie de manzana", "tarta de manzana")) {
            return "apple_pie";
        }

        if (containsAny(text, "red velvet")) {
            return "red_velvet";
        }

        if (containsAny(text, "carrot cake", "torta de zanahoria", "pastel de zanahoria")) {
            return "carrot_cake";
        }

        if (containsAny(text, "alfajor", "alfajores")) {
            return "alfajor";
        }

        if (containsAny(text, "natilla")) {
            return "natilla";
        }

        if (containsAny(text, "llelato", "jelato", "yelato", "gelatto", "gelatoo", "gelato")) {
            return "gelato";
        }

        if (containsAny(text, "sorbete", "sorbet", "sorvete", "sorbeto")) {
            return "sorbet";
        }

        if (containsAny(text, "tiramisuu", "teramisu", "tiramizu", "tiramisu", "tiramisú")) {
            return "tiramisu";
        }

        if (containsAny(text, "pana cota", "panacotta", "pana cotta", "panna cota", "panna cotta", "pannacotta")) {
            return "panna_cotta";
        }

        if (containsAny(text, "crem brule", "creme brule", "creme brulee", "crème brûlée", "crema brule", "crema catalana")) {
            return "creme_brulee";
        }

        if (containsAny(text, "baklava", "baklawa", "baklaba")) {
            return "baklava";
        }

        if (containsAny(text, "cannoli", "cannolo", "canoli", "canolli")) {
            return "cannoli";
        }

        if (containsAny(text, "profiterol", "profiteroles", "profiterole")) {
            return "profiteroles";
        }

        if (containsAny(text, "eclair", "éclair", "eclaire", "eclairs")) {
            return "eclair";
        }

        if (containsAny(text, "pavlova", "pablova")) {
            return "pavlova";
        }

        if (containsAny(text, "mochi", "muchi", "moci")) {
            return "mochi";
        }

        if (containsAny(text, "tresleches", "tres leche", "tres leches", "torta tres leches", "pastel tres leches")) {
            return "tres_leches";
        }

        if (containsAny(text, "arros con leche", "arroz leche", "arroz con lehe", "arroz con leche", "rice pudding")) {
            return "arroz_con_leche";
        }

        if (containsAny(text, "lemom pie", "lemon pai", "lemon pie", "pie limon", "pie de limon", "pie de limón", "tarta de limon", "tarta de limón")) {
            return "lemon_pie";
        }

        if (containsAny(text, "appel pie", "aple pie", "apple pie", "pie manzana", "pie de manzana", "tarta de manzana")) {
            return "apple_pie";
        }

        if (containsAny(text, "red velvet", "terciopelo rojo", "pastel rojo")) {
            return "red_velvet";
        }

        if (containsAny(text, "carrot cake", "torta de zanahoria", "pastel de zanahoria", "cake de zanahoria")) {
            return "carrot_cake";
        }

        if (containsAny(text, "alfajor", "alfajores", "alfaajor")) {
            return "alfajor";
        }

        if (containsAny(text, "natilla", "natilla colombiana", "custard dessert")) {
            return "natilla";
        }

        if (containsAny(text, "churro", "churros", "churros con chocolate")) {
            return "churros";
        }

        if (containsAny(text, "brownie", "brauni", "brownies")) {
            return "brownie";
        }

        if (containsAny(text, "crepe", "crepes", "crepa", "crepas")) {
            return "crepes";
        }

        if (containsAny(text, "waffle", "waffles", "wafle", "wafles")) {
            return "waffle";
        }

        if (containsAny(text, "pancake", "pancakes", "hotcake", "hotcakes")) {
            return "pancakes";
        }

        if (containsAny(text, "macaron", "macarron", "macarrón", "macarons")) {
            return "macaron";
        }

        if (containsAny(text, "mousse", "muse de chocolate", "mousse de chocolate")) {
            return "mousse";
        }

        if (containsAny(text, "parfait", "parfei")) {
            return "parfait";
        }

        if (containsAny(text, "trifle", "traifel")) {
            return "trifle";
        }

        if (containsAny(text, "opera cake", "opera", "pastel opera", "torta opera")) {
            return "opera_cake";
        }

        if (containsAny(text, "black forest", "selva negra", "pastel selva negra", "torta selva negra")) {
            return "black_forest_cake";
        }

        if (containsAny(text, "banoffee", "banoffee pie")) {
            return "banoffee_pie";
        }

        if (containsAny(text, "key lime pie", "pie de lima", "tarta de lima")) {
            return "key_lime_pie";
        }

        if (containsAny(text, "pecan pie", "pie de pecanas", "tarta de pecanas")) {
            return "pecan_pie";
        }

        if (containsAny(text, "pumpkin pie", "pie de calabaza", "tarta de calabaza")) {
            return "pumpkin_pie";
        }

        if (containsAny(text, "pudding", "budin", "budín")) {
            return "pudding";
        }

        if (containsAny(text, "quesillo")) {
            return "quesillo";
        }

        if (containsAny(text, "cake pop", "cake pops", "pop cake")) {
            return "cake_pop";
        }

        if (containsAny(text, "roll cake", "brazo de reina", "brazo gitano", "pionono")) {
            return "roll_cake";
        }

        if (containsAny(text, "cinnamon roll", "rollo de canela", "roles de canela")) {
            return "cinnamon_roll";
        }

        if (containsAny(text, "banana split")) {
            return "banana_split";
        }

        if (containsAny(text, "sundae")) {
            return "sundae";
        }

        if (containsAny(text, "milkshake", "malteada", "batido dulce")) {
            return "milkshake_dessert";
        }

        if (containsAny(text, "buñuelo", "buñuelos")) {
            return "bunuelos";
        }

        if (containsAny(text, "mazamorra dulce", "mazamorra")) {
            return "mazamorra_dulce";
        }

        if (containsAny(text, "brevas con arequipe", "brevas con dulce de leche")) {
            return "brevas_arequipe";
        }

        if (containsAny(text, "buñuelo", "buñuelos", "bunuelos", "bunuelo")) {
            return "bunuelos";
        }

        if (containsAny(text, "mazamorra dulce", "mazamorra", "mazamorra con leche")) {
            return "mazamorra_dulce";
        }

        if (containsAny(text, "brevas con arequipe", "brevas con dulce de leche", "brevas dulces")) {
            return "brevas_arequipe";
        }

        if (containsAny(text, "arroz con coco", "arros con coco", "rice coconut pudding")) {
            return "arroz_con_coco";
        }

        if (containsAny(text, "cocada", "cocadas", "dulce de coco")) {
            return "cocada";
        }

        if (containsAny(text, "marquesa", "marquesa de chocolate", "marquesa venezolana")) {
            return "marquesa";
        }

        if (containsAny(text, "suspiro limeño", "suspiro limeno", "suspiro de limeña", "suspiro de limena")) {
            return "suspiro_limeno";
        }

        if (containsAny(text, "obleas", "oblea", "oblea con arequipe", "obleas con arequipe")) {
            return "oblea_arequipe";
        }

        if (containsAny(text, "dulce de leche", "arequipe", "manjar blanco", "cajeta")) {
            return "dulce_de_leche";
        }

        if (containsAny(text, "flan de coco", "quesillo de coco")) {
            return "flan_coco";
        }

        if (containsAny(text, "torta negra", "torta negra colombiana", "torta negra venezolana")) {
            return "torta_negra";
        }

        if (containsAny(text, "postre de natas", "natas", "postre natas")) {
            return "postre_natas";
        }

        if (containsAny(text, "merengon", "merengón", "merengon de fresa", "merengón de fresa")) {
            return "merengon";
        }

        if (containsAny(text, "chocotorta", "choco torta")) {
            return "chocotorta";
        }

        if (containsAny(text, "brigadeiro", "brigadeiros", "brigadero")) {
            return "brigadeiro";
        }

        if (containsAny(text, "beijinho", "beijinhos", "besito de coco")) {
            return "beijinho";
        }

        if (containsAny(text, "pan de bono dulce", "pandebono dulce", "pandebono")) {
            return "pandebono_dulce";
        }

        if (containsAny(text, "roscon", "roscón", "roscon dulce", "roscón dulce")) {
            return "roscon";
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
            case "gelato" -> "Gelato artesanal";
            case "sorbet" -> "Sorbete artesanal";
            case "tiramisu" -> "Tiramisú clásico";
            case "panna_cotta" -> "Panna cotta cremosa";
            case "baklava" -> "Baklava artesanal";
            case "creme_brulee" -> "Crème brûlée clásica";
            case "cannoli" -> "Cannoli siciliano";
            case "profiteroles" -> "Profiteroles rellenos";
            case "eclair" -> "Éclair de crema";
            case "pavlova" -> "Pavlova con fruta";
            case "mochi" -> "Mochi dulce";
            case "tres_leches" -> "Pastel de tres leches";
            case "arroz_con_leche" -> "Arroz con leche cremoso";
            case "lemon_pie" -> "Lemon pie";
            case "apple_pie" -> "Apple pie";
            case "red_velvet" -> "Red velvet";
            case "carrot_cake" -> "Carrot cake";
            case "alfajor" -> "Alfajor relleno";
            case "natilla" -> "Natilla tradicional";
            case "churros" -> "Churros con azúcar";
            case "brownie" -> "Brownie de chocolate";
            case "crepes" -> "Crepes dulces";
            case "waffle" -> "Waffle dulce";
            case "pancakes" -> "Pancakes dulces";
            case "macaron" -> "Macaron francés";
            case "mousse" -> "Mousse cremoso";
            case "parfait" -> "Parfait en capas";
            case "trifle" -> "Trifle de crema y fruta";
            case "opera_cake" -> "Opera cake";
            case "black_forest_cake" -> "Pastel selva negra";
            case "banoffee_pie" -> "Banoffee pie";
            case "key_lime_pie" -> "Key lime pie";
            case "pecan_pie" -> "Pecan pie";
            case "pumpkin_pie" -> "Pumpkin pie";
            case "pudding" -> "Pudding dulce";
            case "quesillo" -> "Quesillo cremoso";
            case "cake_pop" -> "Cake pop decorado";
            case "roll_cake" -> "Roll cake relleno";
            case "cinnamon_roll" -> "Rollo de canela";
            case "banana_split" -> "Banana split";
            case "sundae" -> "Sundae clásico";
            case "milkshake_dessert" -> "Malteada dulce";
            case "bunuelos" -> "Buñuelos dulces";
            case "mazamorra_dulce" -> "Mazamorra dulce";
            case "brevas_arequipe" -> "Brevas con arequipe";
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
            case "gelato" -> "Postre italiano frío, cremoso y denso, con textura suave y sabor intenso.";
            case "sorbet" -> "Postre helado ligero a base de fruta, fresco y de textura cristalina.";
            case "tiramisu" -> "Postre italiano en capas con crema, café y cacao, suave y aromático.";
            case "panna_cotta" -> "Postre italiano cremoso y delicado, servido frío con salsa dulce o fruta.";
            case "baklava" -> "Postre crujiente de capas finas con frutos secos y almíbar.";
            case "creme_brulee" -> "Postre cremoso con superficie caramelizada y contraste crujiente.";
            case "cannoli" -> "Postre siciliano crujiente relleno de crema dulce, tradicionalmente con ricotta.";
            case "profiteroles" -> "Pequeños bocados de masa choux rellenos de crema y cubiertos con salsa dulce.";
            case "eclair" -> "Pieza alargada de masa choux rellena de crema y cubierta con glaseado.";
            case "pavlova" -> "Postre de merengue crujiente por fuera y suave por dentro, usualmente con fruta.";
            case "mochi" -> "Dulce japonés de textura elástica elaborado con arroz glutinoso.";
            case "tres_leches" -> "Pastel húmedo y suave bañado con mezcla de tres leches.";
            case "arroz_con_leche" -> "Postre cremoso de arroz cocido lentamente con leche, azúcar y canela.";
            case "lemon_pie" -> "Tarta con base crujiente, crema de limón y cubierta de merengue.";
            case "apple_pie" -> "Tarta horneada de manzana con masa dorada y especias suaves.";
            case "red_velvet" -> "Pastel rojo aterciopelado con cacao suave y crema de queso.";
            case "carrot_cake" -> "Pastel especiado de zanahoria con textura húmeda y cobertura cremosa.";
            case "alfajor" -> "Dulce relleno, usualmente con arequipe o dulce de leche, cubierto o espolvoreado.";
            case "natilla" -> "Postre cremoso tradicional, suave y aromático, servido frío o templado.";
            case "churros" -> "Masa frita crujiente espolvoreada con azúcar y canela.";
            case "brownie" -> "Postre denso de chocolate con interior húmedo y superficie ligeramente crujiente.";
            case "crepes" -> "Láminas finas y suaves servidas con rellenos dulces.";
            case "waffle" -> "Postre dorado con textura exterior crujiente y centro suave.";
            case "pancakes" -> "Tortitas dulces esponjosas servidas con miel, frutas o crema.";
            case "macaron" -> "Galleta francesa delicada con relleno cremoso.";
            case "mousse" -> "Postre aireado y cremoso de textura ligera.";
            case "parfait" -> "Postre frío servido en capas con crema, fruta o crujientes.";
            case "trifle" -> "Postre en capas con bizcocho, crema y fruta.";
            case "opera_cake" -> "Pastel francés en capas con café, chocolate y crema.";
            case "black_forest_cake" -> "Pastel de chocolate con crema y cerezas.";
            case "banoffee_pie" -> "Tarta dulce de banana, caramelo y crema.";
            case "key_lime_pie" -> "Tarta cítrica de lima con textura cremosa.";
            case "pecan_pie" -> "Tarta dulce de nueces pecanas con relleno caramelizado.";
            case "pumpkin_pie" -> "Tarta especiada de calabaza con relleno suave.";
            case "pudding" -> "Postre cremoso y suave servido frío o templado.";
            case "quesillo" -> "Postre tipo flan, suave y cremoso, con caramelo.";
            case "cake_pop" -> "Bocado de pastel moldeado en paleta y decorado.";
            case "roll_cake" -> "Bizcocho enrollado con relleno dulce.";
            case "cinnamon_roll" -> "Rollo dulce con canela, glaseado y masa suave.";
            case "banana_split" -> "Postre frío con banana, helado, salsas y toppings.";
            case "sundae" -> "Helado servido con salsa dulce, crema y toppings.";
            case "milkshake_dessert" -> "Bebida dulce cremosa tipo postre, servida fría.";
            case "bunuelos" -> "Bocados fritos tradicionales, dorados y suaves.";
            case "mazamorra_dulce" -> "Postre tradicional cremoso a base de maíz o leche.";
            case "brevas_arequipe" -> "Postre de brevas dulces acompañado con arequipe.";
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
            case "gelato" -> {
                ingredients.add(ingredient("Leche entera", "500", "ml"));
                ingredients.add(ingredient("Crema de leche", "250", "ml"));
                ingredients.add(ingredient("Azúcar", "140", "g"));
                ingredients.add(ingredient("Yemas de huevo", "4", "unidades"));
                ingredients.add(ingredient("Leche en polvo", "35", "g"));
                ingredients.add(ingredient("Vainilla o pasta de sabor", "1", "cucharadita"));
                ingredients.add(ingredient("Estabilizante para helado", "2", "g"));
            }
            case "sorbet" -> {
                ingredients.add(ingredient("Pulpa de fruta", "500", "g"));
                ingredients.add(ingredient("Agua", "220", "ml"));
                ingredients.add(ingredient("Azúcar", "150", "g"));
                ingredients.add(ingredient("Glucosa o miel suave", "40", "g"));
                ingredients.add(ingredient("Jugo de limón", "20", "ml"));
            }
            case "tiramisu" -> {
                ingredients.add(ingredient("Bizcochos de soletilla", "250", "g"));
                ingredients.add(ingredient("Café espresso frío", "300", "ml"));
                ingredients.add(ingredient("Queso mascarpone", "500", "g"));
                ingredients.add(ingredient("Yemas de huevo", "4", "unidades"));
                ingredients.add(ingredient("Azúcar", "120", "g"));
                ingredients.add(ingredient("Cacao en polvo", "25", "g"));
            }
            case "panna_cotta" -> {
                ingredients.add(ingredient("Crema de leche", "500", "ml"));
                ingredients.add(ingredient("Leche", "120", "ml"));
                ingredients.add(ingredient("Azúcar", "90", "g"));
                ingredients.add(ingredient("Gelatina sin sabor", "8", "g"));
                ingredients.add(ingredient("Vainilla", "1", "cucharadita"));
                ingredients.add(ingredient("Salsa de frutos rojos", "120", "g"));
            }
            case "baklava" -> {
                ingredients.add(ingredient("Masa filo", "300", "g"));
                ingredients.add(ingredient("Nueces o pistachos", "250", "g"));
                ingredients.add(ingredient("Mantequilla derretida", "180", "g"));
                ingredients.add(ingredient("Miel", "180", "g"));
                ingredients.add(ingredient("Azúcar", "120", "g"));
                ingredients.add(ingredient("Canela", "1", "cucharadita"));
            }
            case "creme_brulee" -> {
                ingredients.add(ingredient("Crema de leche", "500", "ml"));
                ingredients.add(ingredient("Yemas de huevo", "5", "unidades"));
                ingredients.add(ingredient("Azúcar", "100", "g"));
                ingredients.add(ingredient("Vainilla", "1", "vaina"));
                ingredients.add(ingredient("Azúcar para caramelizar", "60", "g"));
            }
            case "brownie" -> {
                ingredients.add(ingredient("Chocolate oscuro", "200", "g"));
                ingredients.add(ingredient("Mantequilla", "160", "g"));
                ingredients.add(ingredient("Azúcar", "180", "g"));
                ingredients.add(ingredient("Huevos", "3", "unidades"));
                ingredients.add(ingredient("Harina de trigo", "90", "g"));
                ingredients.add(ingredient("Cacao en polvo", "25", "g"));
            }
            case "churros" -> {
                ingredients.add(ingredient("Agua", "250", "ml"));
                ingredients.add(ingredient("Harina de trigo", "160", "g"));
                ingredients.add(ingredient("Mantequilla", "40", "g"));
                ingredients.add(ingredient("Sal", "1", "pizca"));
                ingredients.add(ingredient("Azúcar", "60", "g"));
                ingredients.add(ingredient("Canela", "1", "cucharadita"));
            }
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
                ingredients.add(ingredient("Base principal del postre", "1", "unidad"));
                ingredients.add(ingredient("Crema o relleno", "250", "g"));
                ingredients.add(ingredient("Azúcar", "120", "g"));
                ingredients.add(ingredient("Vainilla o sabor principal", "1", "cucharadita"));
                ingredients.add(ingredient("Decoración comestible", "100", "g"));
            }
        }

        return ingredients;
    }



    private List<Map<String, Object>> fallbackSteps(String dessertType) {
        List<Map<String, Object>> steps = new ArrayList<>();

        switch (dessertType) {
            case "gelato" -> {
                steps.add(step("Preparar la base láctea", "Calentar leche, crema y parte del azúcar sin dejar hervir."));
                steps.add(step("Templar las yemas", "Batir yemas con azúcar e integrar poco a poco la mezcla caliente."));
                steps.add(step("Cocinar la crema", "Cocinar a fuego bajo hasta espesar ligeramente y alcanzar textura de crema inglesa."));
                steps.add(step("Madurar en frío", "Reposar la base en refrigeración durante varias horas para mejorar textura y sabor."));
                steps.add(step("Mantecar y congelar", "Procesar en máquina de helado o congelar batiendo por intervalos hasta obtener gelato cremoso."));
            }
            case "sorbet" -> {
                steps.add(step("Preparar almíbar", "Calentar agua, azúcar y glucosa hasta disolver."));
                steps.add(step("Mezclar con fruta", "Integrar el almíbar frío con la pulpa de fruta y jugo de limón."));
                steps.add(step("Reposar", "Refrigerar la mezcla para estabilizar sabor y textura."));
                steps.add(step("Congelar", "Mantecar o congelar batiendo por intervalos hasta lograr textura ligera."));
            }
            case "tiramisu" -> {
                steps.add(step("Preparar la crema", "Batir yemas con azúcar y mezclar con mascarpone hasta obtener una crema suave."));
                steps.add(step("Remojar bizcochos", "Pasar los bizcochos por café frío sin empaparlos demasiado."));
                steps.add(step("Montar capas", "Alternar capas de bizcocho y crema en un molde."));
                steps.add(step("Refrigerar", "Reposar varias horas para que tome cuerpo."));
                steps.add(step("Finalizar", "Cubrir con cacao justo antes de servir."));
            }
            case "panna_cotta" -> {
                steps.add(step("Hidratar gelatina", "Hidratar la gelatina en agua fría hasta que esté blanda."));
                steps.add(step("Calentar la crema", "Calentar crema, leche, azúcar y vainilla sin hervir."));
                steps.add(step("Integrar gelatina", "Añadir la gelatina hidratada y mezclar hasta disolver."));
                steps.add(step("Moldear", "Verter en moldes y refrigerar hasta cuajar."));
                steps.add(step("Servir", "Desmoldar y acompañar con salsa de fruta o caramelo."));
            }
            case "baklava" -> {
                steps.add(step("Preparar el relleno", "Picar frutos secos y mezclarlos con canela."));
                steps.add(step("Montar capas", "Alternar masa filo con mantequilla y frutos secos."));
                steps.add(step("Cortar", "Marcar porciones antes de hornear."));
                steps.add(step("Hornear", "Hornear hasta que la masa esté dorada y crujiente."));
                steps.add(step("Bañar con almíbar", "Agregar miel o almíbar tibio y dejar reposar."));
            }
            case "creme_brulee" -> {
                steps.add(step("Infusionar la crema", "Calentar crema con vainilla para extraer aroma."));
                steps.add(step("Mezclar yemas", "Batir yemas con azúcar e integrar la crema tibia."));
                steps.add(step("Hornear a baño María", "Cocinar en recipientes individuales hasta que la crema esté firme pero suave."));
                steps.add(step("Enfriar", "Refrigerar hasta que tome textura."));
                steps.add(step("Caramelizar", "Cubrir con azúcar y quemar hasta formar una costra crujiente."));
            }
            default -> {
                steps.add(step("Preparar ingredientes", "Organizar los ingredientes según el tipo de postre solicitado."));
                steps.add(step("Elaborar la base", "Preparar la base, crema, masa o mezcla principal del postre."));
                steps.add(step("Cocinar o enfriar", "Aplicar la técnica adecuada: hornear, freír, refrigerar, congelar o montar en capas."));
                steps.add(step("Reposar", "Permitir que el postre estabilice textura, sabor y presentación."));
                steps.add(step("Decorar", "Agregar acabados visuales comestibles antes de servir."));
            }
        }

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


    private boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value == null) {
            return fallback;
        }

        String text = value.toString().trim().toLowerCase(Locale.ROOT);
        if ("true".equals(text) || "yes".equals(text) || "si".equals(text) || "sí".equals(text)) {
            return true;
        }

        if ("false".equals(text) || "no".equals(text)) {
            return false;
        }

        return fallback;
    }

    private String normalizeForValidation(String value) {
        String lower = String.valueOf(value == null ? "" : value).toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9ñáéíóúü\\s_-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsBlockedContent(String text) {
        return containsAny(text,
                "sexo", "sexual", "porno", "porn", "desnudo", "desnuda", "nude", "xxx",
                "pene", "vagina", "tetas", "culo", "anal", "fetiche",
                "matar", "asesinar", "sangre", "gore", "cadaver", "cuchillo", "arma", "pistola", "bomba",
                "cocaina", "cocaína", "marihuana", "droga", "nazi", "racista",
                "puta", "puto", "mierda", "malparido", "hijueputa", "hp ", " hpta"
        );
    }

    private boolean looksLikeDessertRequest(String text) {
        return containsAny(text,
                "postre", "reposteria", "repostería", "pastel", "torta", "cake", "bizcocho",
                "flan", "dona", "donut", "rosquilla", "helado", "ice cream", "milhojas",
                "cheesecake", "tarta", "pay", "pie", "cupcake", "magdalena", "brownie",
                "galleta", "macaron", "macarron", "macarrón", "chocolate", "fresa", "cereza",
                "arandano", "arándano", "blueberry", "crema", "chantilly", "vainilla",
                "caramelo", "glaseado", "merengue", "mousse", "rosa", "velas", "vela",
                "llelato", "jelato", "yelato", "gelatto", "gelatoo", "gelato", "gelateria", "gelatería", "sorbete", "sorbet", "granita", "semifreddo",
                "tiramisu", "tiramisú", "panna cotta", "pannacotta", "creme brulee", "crema catalana",
                "profiterol", "profiteroles", "eclair", "éclair", "baklava", "churro", "churros",
                "alfajor", "alfajores", "tres leches", "parfait", "mochi", "paleta", "paleta helada"
        );
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
