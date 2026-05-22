package com.backend.pastry3d.composition.service;

import com.backend.pastry3d.composition.entity.CompositionPart;
import com.backend.pastry3d.composition.entity.RecipeComposition;
import com.backend.pastry3d.composition.repository.CompositionPartRepository;
import com.backend.pastry3d.composition.repository.RecipeCompositionRepository;
import com.backend.pastry3d.modelasset.entity.ModelAsset;
import com.backend.pastry3d.shared.constants.AppConstants;
import com.backend.pastry3d.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompositionService {
    private final RecipeCompositionRepository recipeCompositionRepository;
    private final CompositionPartRepository compositionPartRepository;
    private final ObjectMapper objectMapper;

    public CompositionService(RecipeCompositionRepository recipeCompositionRepository,
                              CompositionPartRepository compositionPartRepository,
                              ObjectMapper objectMapper) {
        this.recipeCompositionRepository = recipeCompositionRepository;
        this.compositionPartRepository = compositionPartRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public RecipeComposition getLatestByRecipeId(Long recipeId) {
        return recipeCompositionRepository.findTopByRecipeIdOrderByCreatedAtDesc(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Composición no encontrada"));
    }

    @Transactional
    public RecipeComposition createReuseComposition(Long recipeId, ModelAsset asset) {
        RecipeComposition composition = new RecipeComposition();
        composition.setRecipeId(recipeId);
        composition.setStrategy(AppConstants.STRATEGY_REUSE_COMPLETE_MODEL);
        composition.setCompleteModelAssetId(asset.getId());
        composition.setStatus(AppConstants.STATUS_READY);
        composition.setSceneJson(safeJson(Map.of(
                "strategy", AppConstants.STRATEGY_REUSE_COMPLETE_MODEL,
                "modelUrl", asset.getModelUrl(),
                "assetKey", asset.getAssetKey()
        )));
        return recipeCompositionRepository.save(composition);
    }

    @Transactional
    public RecipeComposition createPendingManualComposition(Long recipeId, String missingAssetsJson) {
        RecipeComposition composition = new RecipeComposition();
        composition.setRecipeId(recipeId);
        composition.setStrategy(AppConstants.STRATEGY_PENDING_MANUAL_MODEL);
        composition.setStatus(AppConstants.STATUS_PENDING_MANUAL_MODEL);
        composition.setMissingAssetsJson(missingAssetsJson);
        composition.setSceneJson("{}");
        return recipeCompositionRepository.save(composition);
    }

    @Transactional
    public RecipeComposition createSceneComposition(Long recipeId, ModelAsset base, List<ScenePartRequest> parts) {
        RecipeComposition composition = new RecipeComposition();
        composition.setRecipeId(recipeId);
        composition.setStrategy(AppConstants.STRATEGY_COMPOSE_SCENE);
        composition.setBaseAssetId(base.getId());
        composition.setStatus(AppConstants.STATUS_READY);
        RecipeComposition saved = recipeCompositionRepository.save(composition);

        List<Map<String, Object>> sceneParts = new ArrayList<>();
        for (ScenePartRequest request : parts) {
            CompositionPart part = new CompositionPart();
            part.setCompositionId(saved.getId());
            part.setAssetId(request.getAsset().getId());
            part.setAnchor(request.getAnchor());
            part.setPositionJson(safeJson(request.getPosition()));
            part.setScale(request.getScale());
            part.setRotationJson(safeJson(request.getRotation()));
            part.setColorOverridesJson(safeJson(request.getColorOverrides()));
            compositionPartRepository.save(part);

            Map<String, Object> partJson = new HashMap<>();
            partJson.put("assetId", request.getAsset().getId());
            partJson.put("assetKey", request.getAsset().getAssetKey());
            partJson.put("modelUrl", request.getAsset().getModelUrl());
            partJson.put("anchor", request.getAnchor());
            partJson.put("position", request.getPosition());
            partJson.put("rotation", request.getRotation());
            partJson.put("scale", request.getScale());
            partJson.put("colorOverrides", request.getColorOverrides());
            sceneParts.add(partJson);
        }

        Map<String, Object> scene = new HashMap<>();
        scene.put("strategy", AppConstants.STRATEGY_COMPOSE_SCENE);
        scene.put("base", Map.of(
                "assetId", base.getId(),
                "assetKey", base.getAssetKey(),
                "modelUrl", base.getModelUrl()
        ));
        scene.put("parts", sceneParts);
        saved.setSceneJson(safeJson(scene));
        return recipeCompositionRepository.save(saved);
    }

    private String safeJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception exception) {
            return "{}";
        }
    }

    public static class ScenePartRequest {
        private ModelAsset asset;
        private String anchor;
        private Map<String, Object> position;
        private Map<String, Object> rotation;
        private Double scale;
        private Map<String, Object> colorOverrides;

        public ModelAsset getAsset() { return asset; }
        public void setAsset(ModelAsset asset) { this.asset = asset; }
        public String getAnchor() { return anchor; }
        public void setAnchor(String anchor) { this.anchor = anchor; }
        public Map<String, Object> getPosition() { return position; }
        public void setPosition(Map<String, Object> position) { this.position = position; }
        public Map<String, Object> getRotation() { return rotation; }
        public void setRotation(Map<String, Object> rotation) { this.rotation = rotation; }
        public Double getScale() { return scale; }
        public void setScale(Double scale) { this.scale = scale; }
        public Map<String, Object> getColorOverrides() { return colorOverrides; }
        public void setColorOverrides(Map<String, Object> colorOverrides) { this.colorOverrides = colorOverrides; }
    }
}
