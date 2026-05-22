package com.backend.pastry3d.recipe.service;

import com.backend.pastry3d.ai.gemini.GeminiClient;
import com.backend.pastry3d.auth.entity.User;
import com.backend.pastry3d.composition.entity.RecipeComposition;
import com.backend.pastry3d.composition.repository.RecipeCompositionRepository;
import com.backend.pastry3d.composition.service.CompositionService;
import com.backend.pastry3d.generation.service.GenerationJobService;
import com.backend.pastry3d.modelasset.entity.ModelAsset;
import com.backend.pastry3d.modelasset.repository.ModelAssetRepository;
import com.backend.pastry3d.recipe.dto.RecipeDetailResponse;
import com.backend.pastry3d.recipe.dto.RecipeGenerateResponse;
import com.backend.pastry3d.recipe.entity.Recipe;
import com.backend.pastry3d.recipe.repository.RecipeRepository;
import com.backend.pastry3d.shared.constants.AppConstants;
import com.backend.pastry3d.shared.exception.BadRequestException;
import com.backend.pastry3d.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeCompositionRepository recipeCompositionRepository;
    private final ModelAssetRepository modelAssetRepository;
    private final CompositionService compositionService;
    private final GeminiClient geminiClient;
    private final GenerationJobService generationJobService;
    private final ObjectMapper objectMapper;

    public RecipeService(
            RecipeRepository recipeRepository,
            RecipeCompositionRepository recipeCompositionRepository,
            ModelAssetRepository modelAssetRepository,
            CompositionService compositionService,
            GeminiClient geminiClient,
            GenerationJobService generationJobService,
            ObjectMapper objectMapper
    ) {
        this.recipeRepository = recipeRepository;
        this.recipeCompositionRepository = recipeCompositionRepository;
        this.modelAssetRepository = modelAssetRepository;
        this.compositionService = compositionService;
        this.geminiClient = geminiClient;
        this.generationJobService = generationJobService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RecipeGenerateResponse generateRecipe(String prompt, User user) {
        if (prompt == null || prompt.isBlank()) {
            throw new BadRequestException("El prompt de la receta es obligatorio");
        }

        Map<String, Object> plan = geminiClient.generateRecipePlan(prompt);
        Map<String, Object> recipeMap = castMap(plan.get("recipe"));
        Map<String, Object> visualPlan = castMap(plan.get("visualPlan"));

        Recipe recipe = new Recipe();
        recipe.setUserId(user.getId());
        recipe.setPrompt(prompt);
        recipe.setTitle(stringValue(recipeMap.get("title"), "Receta Pastry3D"));
        recipe.setDessertType(stringValue(recipeMap.get("dessertType"), stringValue(visualPlan.get("dessertType"), "generic_dessert")));
        recipe.setDifficulty(stringValue(recipeMap.get("difficulty"), "BEGINNER"));
        recipe.setServings(intValue(recipeMap.get("servings"), 8));
        recipe.setNormalizedPrompt(stringValue(visualPlan.get("normalizedPrompt"), prompt));
        recipe.setModelPrompt(stringValue(visualPlan.get("modelPrompt"), prompt));
        recipe.setRecipeJson(safeJson(recipeMap));
        recipe.setVisualPlanJson(safeJson(visualPlan));
        recipe.setStatus(AppConstants.STATUS_PENDING);

        Recipe savedRecipe = recipeRepository.save(recipe);

        RecipeComposition composition = buildComposition(savedRecipe, visualPlan);

        savedRecipe.setStatus(composition.getStatus());
        recipeRepository.save(savedRecipe);

        return buildGenerateResponse(savedRecipe, composition);
    }

    @Transactional(readOnly = true)
    public List<Recipe> findMine(Long userId) {
        return recipeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public RecipeDetailResponse getDetail(Long id, Long userId) {
        Recipe recipe = getOwnedRecipe(id, userId);

        RecipeComposition composition = recipeCompositionRepository
                .findTopByRecipeIdOrderByCreatedAtDesc(recipe.getId())
                .orElse(null);

        RecipeDetailResponse response = new RecipeDetailResponse();
        response.setId(recipe.getId());
        response.setTitle(recipe.getTitle());
        response.setPrompt(recipe.getPrompt());
        response.setDessertType(recipe.getDessertType());
        response.setDifficulty(recipe.getDifficulty());
        response.setServings(recipe.getServings());
        response.setStatus(recipe.getStatus());
        response.setRecipeJson(recipe.getRecipeJson());
        response.setVisualPlanJson(recipe.getVisualPlanJson());
        response.setModelPrompt(recipe.getModelPrompt());

        if (composition != null) {
            response.setStrategy(composition.getStrategy());
            response.setSceneJson(composition.getSceneJson());

            if (composition.getCompleteModelAssetId() != null) {
                modelAssetRepository.findById(composition.getCompleteModelAssetId())
                        .ifPresent(asset -> response.setModelUrl(asset.getModelUrl()));
            }
        }

        return response;
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Recipe recipe = getOwnedRecipe(id, userId);
        recipeRepository.delete(recipe);
    }

    @Transactional
    public RecipeGenerateResponse rebuildComposition(Long id, Long userId) {
        Recipe recipe = getOwnedRecipe(id, userId);
        Map<String, Object> visualPlan = readMap(recipe.getVisualPlanJson());

        RecipeComposition composition = buildComposition(recipe, visualPlan);

        recipe.setStatus(composition.getStatus());
        recipeRepository.save(recipe);

        return buildGenerateResponse(recipe, composition);
    }

    private RecipeGenerateResponse buildGenerateResponse(Recipe recipe, RecipeComposition composition) {
        RecipeGenerateResponse response = new RecipeGenerateResponse();
        response.setRecipeId(recipe.getId());
        response.setTitle(recipe.getTitle());
        response.setStatus(recipe.getStatus());
        response.setStrategy(composition.getStrategy());
        response.setSceneJson(composition.getSceneJson());
        response.setRecipeJson(recipe.getRecipeJson());
        response.setVisualPlanJson(recipe.getVisualPlanJson());
        response.setModelPrompt(recipe.getModelPrompt());
        response.setMissingAssetsJson(composition.getMissingAssetsJson());

        if (composition.getCompleteModelAssetId() != null) {
            modelAssetRepository.findById(composition.getCompleteModelAssetId())
                    .ifPresent(asset -> response.setModelUrl(asset.getModelUrl()));
        }

        return response;
    }

    private RecipeComposition buildComposition(Recipe recipe, Map<String, Object> visualPlan) {
        String dessertType = recipe.getDessertType();
        List<Map<String, Object>> requiredAssets = extractRequiredAssets(visualPlan);

        Optional<ModelAsset> baseAsset = findBaseAsset(dessertType);
        Optional<ModelAsset> completeModel = findCompleteModel(dessertType);

        if (baseAsset.isEmpty() && completeModel.isPresent() && requiredAssets.isEmpty()) {
            return compositionService.createReuseComposition(recipe.getId(), completeModel.get());
        }

        ModelAsset baseForScene = null;

        if (baseAsset.isPresent()) {
            baseForScene = baseAsset.get();
        } else if (completeModel.isPresent()) {
            baseForScene = completeModel.get();
        }

        if (baseForScene == null) {
            return handleMissingAssets(recipe, List.of(dessertType + "_base_or_complete_model"));
        }

        List<CompositionService.ScenePartRequest> sceneParts = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (Map<String, Object> required : requiredAssets) {
            Optional<ModelAsset> matchedAsset = matchRequiredAsset(required);

            if (matchedAsset.isEmpty()) {
                missing.add(resolveMissingLabel(required));
                continue;
            }

            CompositionService.ScenePartRequest part = buildScenePart(required, matchedAsset.get());
            sceneParts.add(part);
        }

        if (!missing.isEmpty()) {
            return handleMissingAssets(recipe, missing);
        }

        return compositionService.createSceneComposition(recipe.getId(), baseForScene, sceneParts);
    }

    private Optional<ModelAsset> findBaseAsset(String dessertType) {
        return modelAssetRepository.findFirstByDessertTypeAndCategoryAndStatusOrderByQualityScoreDesc(
                dessertType,
                AppConstants.CATEGORY_BASE,
                AppConstants.STATUS_READY
        );
    }

    private Optional<ModelAsset> findCompleteModel(String dessertType) {
        return modelAssetRepository.findFirstByDessertTypeAndCategoryAndStatusOrderByQualityScoreDesc(
                dessertType,
                AppConstants.CATEGORY_COMPLETE_DESSERT,
                AppConstants.STATUS_READY
        );
    }

    private Optional<ModelAsset> matchRequiredAsset(Map<String, Object> required) {
        String assetKey = stringValue(required.get("assetKey"), null);

        if (assetKey != null && !assetKey.isBlank()) {
            Optional<ModelAsset> exact = modelAssetRepository.findByAssetKey(assetKey);

            if (exact.isPresent() && AppConstants.STATUS_READY.equals(exact.get().getStatus())) {
                return exact;
            }
        }

        String category = stringValue(required.get("category"), AppConstants.CATEGORY_TOPPING);
        String type = stringValue(required.get("type"), "");
        String query = stringValue(required.get("query"), "");

        String textToMatch = (safeText(assetKey) + " " + type + " " + query).toLowerCase();

        List<ModelAsset> candidates = modelAssetRepository.findByCategoryAndStatusOrderByQualityScoreDesc(
                category,
                AppConstants.STATUS_READY
        );

        ModelAsset best = null;
        int bestScore = 0;

        for (ModelAsset candidate : candidates) {
            int score = scoreAsset(candidate, textToMatch);

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best != null && bestScore > 0) {
            return Optional.of(best);
        }

        return Optional.empty();
    }

    private int scoreAsset(ModelAsset asset, String query) {
        String searchable = (
                safeText(asset.getAssetKey()) + " "
                        + safeText(asset.getName()) + " "
                        + safeText(asset.getTags()) + " "
                        + safeText(asset.getDessertType()) + " "
                        + safeText(asset.getCategory())
        ).toLowerCase();

        int score = 0;

        for (String token : query.split("[ ,;:_\\-]+")) {
            if (token == null || token.isBlank()) {
                continue;
            }

            String normalized = token.trim().toLowerCase();

            if (normalized.length() < 3) {
                continue;
            }

            if (searchable.contains(normalized)) {
                score++;
            }
        }

        return score;
    }

    private CompositionService.ScenePartRequest buildScenePart(Map<String, Object> required, ModelAsset asset) {
        CompositionService.ScenePartRequest part = new CompositionService.ScenePartRequest();

        String anchor = stringValue(
                required.get("anchor"),
                stringValue(required.get("placement"), "top_center")
        );

        String relativeSize = stringValue(required.get("relativeSize"), "small");
        String color = stringValue(required.get("color"), null);
        String materialTarget = stringValue(required.get("materialTarget"), defaultMaterialTarget(required, asset));

        part.setAsset(asset);
        part.setAnchor(anchor);
        part.setPosition(defaultPosition(anchor));
        part.setRotation(defaultRotation());
        part.setScale(defaultScale(asset.getCategory(), relativeSize));
        part.setColorOverrides(buildColorOverrides(materialTarget, color));

        return part;
    }

    private Map<String, Object> buildColorOverrides(String materialTarget, String color) {
        if (color == null || color.isBlank() || "null".equalsIgnoreCase(color)) {
            return Map.of();
        }

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put(materialTarget, color);

        if (!"primary".equalsIgnoreCase(materialTarget)) {
            overrides.put("primary", color);
        }

        return overrides;
    }

    private String defaultMaterialTarget(Map<String, Object> required, ModelAsset asset) {
        String type = stringValue(required.get("type"), "").toLowerCase();
        String assetKey = safeText(asset.getAssetKey()).toLowerCase();

        if (type.contains("rose") || type.contains("rosa") || assetKey.contains("rose") || assetKey.contains("rosa")) {
            return "petals";
        }

        if (type.contains("cream") || type.contains("crema") || assetKey.contains("cream")) {
            return "cream";
        }

        if (type.contains("chocolate") || assetKey.contains("chocolate")) {
            return "chocolate";
        }

        if (type.contains("candle") || type.contains("vela") || assetKey.contains("candle")) {
            return "primary";
        }

        return "primary";
    }

    private RecipeComposition handleMissingAssets(Recipe recipe, List<String> missing) {
        generationJobService.createPendingManualJob(recipe.getId(), recipe.getModelPrompt());

        return compositionService.createPendingManualComposition(
                recipe.getId(),
                safeJson(missing)
        );
    }

    private Recipe getOwnedRecipe(Long id, Long userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        if (!recipe.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Receta no encontrada");
        }

        return recipe;
    }

    private Map<String, Object> defaultPosition(String anchor) {
        return switch (anchor) {
            case "top_left" -> Map.of("x", -0.65, "y", 1.15, "z", 0.0);
            case "top_right" -> Map.of("x", 0.65, "y", 1.15, "z", 0.0);
            case "top_ring" -> Map.of("x", 0.45, "y", 1.15, "z", 0.0);
            case "top_back" -> Map.of("x", 0.0, "y", 1.35, "z", -0.35);
            case "side_front" -> Map.of("x", 0.0, "y", 0.65, "z", 0.85);
            case "top_drizzle" -> Map.of("x", 0.0, "y", 1.18, "z", 0.0);
            default -> Map.of("x", 0.0, "y", 1.15, "z", 0.0);
        };
    }

    private Map<String, Object> defaultRotation() {
        return Map.of("x", 0.0, "y", 0.0, "z", 0.0);
    }

    private Double defaultScale(String category, String relativeSize) {
        double baseScale;

        if (AppConstants.CATEGORY_DECORATION.equals(category)) {
            baseScale = 0.35;
        } else if (AppConstants.CATEGORY_SAUCE.equals(category)) {
            baseScale = 0.9;
        } else if (AppConstants.CATEGORY_SPRINKLES.equals(category)) {
            baseScale = 0.8;
        } else {
            baseScale = 0.4;
        }

        if ("large".equalsIgnoreCase(relativeSize)) {
            return baseScale * 1.35;
        }

        if ("medium".equalsIgnoreCase(relativeSize)) {
            return baseScale * 1.1;
        }

        return baseScale;
    }

    private String resolveMissingLabel(Map<String, Object> required) {
        String assetKey = stringValue(required.get("assetKey"), null);

        if (assetKey != null && !assetKey.isBlank()) {
            return assetKey;
        }

        String type = stringValue(required.get("type"), null);

        if (type != null && !type.isBlank()) {
            return type;
        }

        String query = stringValue(required.get("query"), null);

        if (query != null && !query.isBlank()) {
            return query;
        }

        return stringValue(required.get("category"), "unknown_asset");
    }

    private List<Map<String, Object>> extractRequiredAssets(Map<String, Object> visualPlan) {
        Object value = visualPlan.get("requiredAssets");

        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> converted = new HashMap<>();

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    converted.put(String.valueOf(entry.getKey()), entry.getValue());
                }

                result.add(converted);
            }
        }

        return result;
    }

    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new HashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            return converted;
        }

        return new HashMap<>();
    }

    private Map<String, Object> readMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new HashMap<>();
            }

            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new BadRequestException("No se pudo leer el plan visual de la receta");
        }
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
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

    private Integer intValue(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return value == null ? fallback : Integer.parseInt(value.toString());
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}