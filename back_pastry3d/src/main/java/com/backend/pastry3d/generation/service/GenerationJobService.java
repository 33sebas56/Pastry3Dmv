package com.backend.pastry3d.generation.service;

import com.backend.pastry3d.adapters.fairstack.FairStackClient;
import com.backend.pastry3d.generation.entity.GenerationJob;
import com.backend.pastry3d.generation.repository.GenerationJobRepository;
import com.backend.pastry3d.modelasset.entity.ModelAsset;
import com.backend.pastry3d.modelasset.repository.ModelAssetRepository;
import com.backend.pastry3d.recipe.entity.Recipe;
import com.backend.pastry3d.recipe.repository.RecipeRepository;
import com.backend.pastry3d.shared.constants.AppConstants;
import com.backend.pastry3d.shared.exception.BadRequestException;
import com.backend.pastry3d.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class GenerationJobService {

    private final GenerationJobRepository generationJobRepository;
    private final RecipeRepository recipeRepository;
    private final ModelAssetRepository modelAssetRepository;
    private final FairStackClient fairStackClient;
    private final ObjectMapper objectMapper;
    private final boolean fairStackEnabled;
    private final String storageUploads;

    public GenerationJobService(
            GenerationJobRepository generationJobRepository,
            RecipeRepository recipeRepository,
            ModelAssetRepository modelAssetRepository,
            FairStackClient fairStackClient,
            ObjectMapper objectMapper,
            @Value("${fairstack.enabled:false}") boolean fairStackEnabled,
            @Value("${storage.uploads:uploads/models}") String storageUploads
    ) {
        this.generationJobRepository = generationJobRepository;
        this.recipeRepository = recipeRepository;
        this.modelAssetRepository = modelAssetRepository;
        this.fairStackClient = fairStackClient;
        this.objectMapper = objectMapper;
        this.fairStackEnabled = fairStackEnabled;
        this.storageUploads = storageUploads;
    }

    @Transactional
    public GenerationJob createPendingManualJob(Long recipeId, String modelPrompt) {
        GenerationJob job = new GenerationJob();
        job.setRecipeId(recipeId);
        job.setProvider("MANUAL_OR_FAIRSTACK");
        job.setStatus(AppConstants.STATUS_PENDING_MANUAL_MODEL);
        job.setRequestJson(safeJson(Map.of("modelPrompt", modelPrompt)));
        return generationJobRepository.save(job);
    }

    @Transactional
    public GenerationJob startFairStackForRecipe(Long recipeId, Long userId) {
        if (!fairStackEnabled) {
            throw new BadRequestException("FairStack está desactivado");
        }

        Recipe recipe = getOwnedRecipe(recipeId, userId);

        String prompt = recipe.getModelPrompt();

        if (prompt == null || prompt.isBlank()) {
            prompt = recipe.getPrompt();
        }

        if (prompt == null || prompt.isBlank()) {
            throw new BadRequestException("La receta no tiene prompt para generar modelo 3D");
        }

        String assetKey = buildGeneratedAssetKey(recipe);

        GenerationJob job = new GenerationJob();
        job.setRecipeId(recipe.getId());
        job.setProvider("FAIRSTACK");
        job.setStatus(AppConstants.STATUS_GENERATING);
        job.setRequestJson(safeJson(Map.of(
                "recipeId", recipe.getId(),
                "assetKey", assetKey,
                "prompt", prompt
        )));

        GenerationJob saved = generationJobRepository.save(job);

        try {
            Map<String, Object> response = fairStackClient.startTextTo3D(prompt, recipe.getId(), assetKey);

            saved.setResponseJson(safeJson(response));

            String providerJobId = fairStackClient.extractProviderJobId(response);
            if (providerJobId != null && !providerJobId.isBlank()) {
                saved.setProviderJobId(providerJobId);
            }

            String modelUrl = fairStackClient.extractModelUrl(response);

            if (modelUrl != null && !modelUrl.isBlank()) {
                ModelAsset asset = saveGeneratedAsset(recipe, assetKey, modelUrl);
                saved.setModelAssetId(asset.getId());
                saved.setStatus(AppConstants.STATUS_READY);
                recipe.setStatus(AppConstants.STATUS_READY);
                recipeRepository.save(recipe);
            } else {
                saved.setStatus(AppConstants.STATUS_GENERATING);
            }

            return generationJobRepository.save(saved);
        } catch (Exception exception) {
            saved.setStatus(AppConstants.STATUS_FAILED);
            saved.setErrorMessage(exception.getMessage());
            return generationJobRepository.save(saved);
        }
    }

    @Transactional(readOnly = true)
    public GenerationJob getJob(Long id) {
        return generationJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job de generación no encontrado"));
    }

    @Transactional
    public GenerationJob syncJob(Long id, Long userId) {
        GenerationJob job = generationJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job de generación no encontrado"));

        Recipe recipe = getOwnedRecipe(job.getRecipeId(), userId);

        if (!"FAIRSTACK".equals(job.getProvider())) {
            return job;
        }

        if (job.getProviderJobId() == null || job.getProviderJobId().isBlank()) {
            return job;
        }

        try {
            Map<String, Object> response = fairStackClient.getJobStatus(job.getProviderJobId());
            job.setResponseJson(safeJson(response));

            String status = readStatus(response);
            if (status != null) {
                job.setStatus(status);
            }

            String modelUrl = fairStackClient.extractModelUrl(response);

            if (modelUrl != null && !modelUrl.isBlank() && job.getModelAssetId() == null) {
                String assetKey = buildGeneratedAssetKey(recipe);
                ModelAsset asset = saveGeneratedAsset(recipe, assetKey, modelUrl);
                job.setModelAssetId(asset.getId());
                job.setStatus(AppConstants.STATUS_READY);
                recipe.setStatus(AppConstants.STATUS_READY);
                recipeRepository.save(recipe);
            }

            return generationJobRepository.save(job);
        } catch (Exception exception) {
            job.setErrorMessage(exception.getMessage());
            return generationJobRepository.save(job);
        }
    }

    private Recipe getOwnedRecipe(Long recipeId, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada"));

        if (!recipe.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Receta no encontrada");
        }

        return recipe;
    }

    private ModelAsset saveGeneratedAsset(Recipe recipe, String assetKey, String remoteModelUrl) {
        try {
            byte[] glbBytes = fairStackClient.downloadBinary(remoteModelUrl);

            Path directory = Path.of(storageUploads, "generated");
            Files.createDirectories(directory);

            Path filePath = directory.resolve(assetKey + ".glb");
            Files.write(filePath, glbBytes);

            String publicUrl = "/uploads/models/generated/" + assetKey + ".glb";

            ModelAsset asset = new ModelAsset();
            asset.setAssetKey(assetKey);
            asset.setName("Modelo generado para " + safeText(recipe.getTitle(), "receta"));
            asset.setCategory(AppConstants.CATEGORY_COMPLETE_DESSERT);
            asset.setDessertType(recipe.getDessertType());
            asset.setModelUrl(publicUrl);
            asset.setProvider("FAIRSTACK");
            asset.setStatus(AppConstants.STATUS_READY);
            asset.setQualityScore(0.60);
            asset.setTags(buildTags(recipe));
            asset.setColorMode("FIXED");
            asset.setMaterialSlotsJson("{}");

            return modelAssetRepository.save(asset);
        } catch (Exception exception) {
            throw new BadRequestException("FairStack generó respuesta, pero no se pudo guardar el GLB: " + exception.getMessage());
        }
    }

    private String buildGeneratedAssetKey(Recipe recipe) {
        String dessertType = recipe.getDessertType() == null || recipe.getDessertType().isBlank()
                ? "dessert"
                : recipe.getDessertType();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        return sanitize("generated_" + dessertType + "_recipe_" + recipe.getId() + "_" + timestamp);
    }

    private String buildTags(Recipe recipe) {
        StringBuilder builder = new StringBuilder();
        builder.append("generated,fairstack,ai,complete_dessert");

        if (recipe.getDessertType() != null) {
            builder.append(",").append(recipe.getDessertType());
        }

        if (recipe.getTitle() != null) {
            builder.append(",").append(recipe.getTitle().toLowerCase());
        }

        return builder.toString();
    }

    private String readStatus(Map<String, Object> response) {
        Object status = response.get("status");

        if (status == null) {
            status = response.get("state");
        }

        if (status == null) {
            return null;
        }

        String normalized = status.toString().trim().toUpperCase();

        if (normalized.contains("SUCCESS") || normalized.contains("FINISHED") || normalized.contains("COMPLETED") || normalized.contains("READY")) {
            return AppConstants.STATUS_READY;
        }

        if (normalized.contains("FAIL") || normalized.contains("ERROR")) {
            return AppConstants.STATUS_FAILED;
        }

        return AppConstants.STATUS_GENERATING;
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "generated_model";
        }

        return value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]", "_");
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }
}