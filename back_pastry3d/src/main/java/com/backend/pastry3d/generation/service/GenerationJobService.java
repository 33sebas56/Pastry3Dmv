package com.backend.pastry3d.generation.service;

import com.backend.pastry3d.adapters.fairstack.FairStackClient;
import com.backend.pastry3d.adapters.tripo.TripoClient;
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
import java.util.List;
import java.util.Map;

@Service
public class GenerationJobService {

    private static final String PROVIDER_FAIRSTACK = "FAIRSTACK";
    private static final String PROVIDER_TRIPO = "TRIPO";

    private final GenerationJobRepository generationJobRepository;
    private final RecipeRepository recipeRepository;
    private final ModelAssetRepository modelAssetRepository;
    private final FairStackClient fairStackClient;
    private final TripoClient tripoClient;
    private final ObjectMapper objectMapper;
    private final boolean fairStackEnabled;
    private final boolean tripoEnabled;
    private final String storageUploads;

    public GenerationJobService(
            GenerationJobRepository generationJobRepository,
            RecipeRepository recipeRepository,
            ModelAssetRepository modelAssetRepository,
            FairStackClient fairStackClient,
            TripoClient tripoClient,
            ObjectMapper objectMapper,
            @Value("${fairstack.enabled:false}") boolean fairStackEnabled,
            @Value("${tripo.enabled:false}") boolean tripoEnabled,
            @Value("${storage.uploads:uploads/models}") String storageUploads
    ) {
        this.generationJobRepository = generationJobRepository;
        this.recipeRepository = recipeRepository;
        this.modelAssetRepository = modelAssetRepository;
        this.fairStackClient = fairStackClient;
        this.tripoClient = tripoClient;
        this.objectMapper = objectMapper;
        this.fairStackEnabled = fairStackEnabled;
        this.tripoEnabled = tripoEnabled;
        this.storageUploads = storageUploads;
    }

    @Transactional
    public GenerationJob createPendingManualJob(Long recipeId, String modelPrompt) {
        GenerationJob job = new GenerationJob();
        job.setRecipeId(recipeId);
        job.setProvider("MANUAL_OR_EXTERNAL_3D");
        job.setStatus(AppConstants.STATUS_PENDING_MANUAL_MODEL);
        job.setRequestJson(safeJson(Map.of("modelPrompt", safeText(modelPrompt, ""))));
        return generationJobRepository.save(job);
    }

    @Transactional
    public GenerationJob startTripoForRecipe(Long recipeId, Long userId) {
        if (!tripoEnabled) {
            throw new BadRequestException("Tripo está desactivado. Activa TRIPO_ENABLED=true en .env");
        }

        Recipe recipe = getOwnedRecipe(recipeId, userId);

        GenerationJob reusableJob = findReusableTripoJob(recipe.getId());
        if (reusableJob != null) {
            return reusableJob;
        }

        String prompt = buildExternalPrompt(recipe);
        String assetKey = buildGeneratedAssetKey(recipe);

        GenerationJob job = new GenerationJob();
        job.setRecipeId(recipe.getId());
        job.setProvider(PROVIDER_TRIPO);
        job.setStatus(AppConstants.STATUS_GENERATING);
        job.setRequestJson(safeJson(Map.of(
                "recipeId", recipe.getId(),
                "assetKey", assetKey,
                "prompt", prompt
        )));

        GenerationJob saved = generationJobRepository.save(job);

        try {
            Map<String, Object> response = tripoClient.startTextToModel(prompt, recipe.getId(), assetKey);
            saved.setResponseJson(safeJson(response));

            String providerJobId = tripoClient.extractTaskId(response);
            if (providerJobId == null || providerJobId.isBlank()) {
                throw new BadRequestException("Tripo no devolvió task_id");
            }

            saved.setProviderJobId(providerJobId);
            saved.setStatus(AppConstants.STATUS_GENERATING);
            return generationJobRepository.save(saved);
        } catch (Exception exception) {
            saved.setStatus(AppConstants.STATUS_FAILED);
            saved.setErrorMessage(cleanError(exception.getMessage()));
            return generationJobRepository.save(saved);
        }
    }


    private GenerationJob findReusableTripoJob(Long recipeId) {
        List<GenerationJob> existingJobs = generationJobRepository.findByRecipeIdAndProviderAndStatusInOrderByCreatedAtDesc(
                recipeId,
                PROVIDER_TRIPO,
                List.of(AppConstants.STATUS_GENERATING, AppConstants.STATUS_READY)
        );

        if (existingJobs == null || existingJobs.isEmpty()) {
            return null;
        }

        return existingJobs.get(0);
    }

    @Transactional
    public GenerationJob startFairStackForRecipe(Long recipeId, Long userId) {
        if (!fairStackEnabled) {
            throw new BadRequestException("FairStack está desactivado");
        }

        Recipe recipe = getOwnedRecipe(recipeId, userId);
        String prompt = buildExternalPrompt(recipe);
        String assetKey = buildGeneratedAssetKey(recipe);

        GenerationJob job = new GenerationJob();
        job.setRecipeId(recipe.getId());
        job.setProvider(PROVIDER_FAIRSTACK);
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
                ModelAsset asset = saveGeneratedAsset(recipe, assetKey, modelUrl, PROVIDER_FAIRSTACK);
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
            saved.setErrorMessage(cleanError(exception.getMessage()));
            return generationJobRepository.save(saved);
        }
    }


    @Transactional(readOnly = true)
    public GenerationJob getActiveTripoJobForRecipe(Long recipeId, Long userId) {
        Recipe recipe = getOwnedRecipe(recipeId, userId);
        return findReusableTripoJob(recipe.getId());
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

        if (PROVIDER_TRIPO.equals(job.getProvider())) {
            return syncTripoJob(job, recipe);
        }

        if (PROVIDER_FAIRSTACK.equals(job.getProvider())) {
            return syncFairStackJob(job, recipe);
        }

        return job;
    }

    private GenerationJob syncTripoJob(GenerationJob job, Recipe recipe) {
        if (job.getProviderJobId() == null || job.getProviderJobId().isBlank()) {
            return job;
        }

        try {
            Map<String, Object> response = tripoClient.getTask(job.getProviderJobId());
            job.setResponseJson(safeJson(response));

            String status = normalizeProviderStatus(tripoClient.extractStatus(response));
            if (status != null) {
                job.setStatus(status);
            }

            String modelUrl = tripoClient.extractModelUrl(response);

            if (modelUrl != null && !modelUrl.isBlank() && job.getModelAssetId() == null) {
                String assetKey = buildGeneratedAssetKey(recipe);
                ModelAsset asset = saveGeneratedAsset(recipe, assetKey, modelUrl, PROVIDER_TRIPO);
                job.setModelAssetId(asset.getId());
                job.setStatus(AppConstants.STATUS_READY);
                recipe.setStatus(AppConstants.STATUS_READY);
                recipeRepository.save(recipe);
            }

            return generationJobRepository.save(job);
        } catch (Exception exception) {
            job.setErrorMessage(cleanError(exception.getMessage()));
            return generationJobRepository.save(job);
        }
    }

    private GenerationJob syncFairStackJob(GenerationJob job, Recipe recipe) {
        if (job.getProviderJobId() == null || job.getProviderJobId().isBlank()) {
            return job;
        }

        try {
            Map<String, Object> response = fairStackClient.getJobStatus(job.getProviderJobId());
            job.setResponseJson(safeJson(response));

            String status = normalizeProviderStatus(readStatus(response));
            if (status != null) {
                job.setStatus(status);
            }

            String modelUrl = fairStackClient.extractModelUrl(response);

            if (modelUrl != null && !modelUrl.isBlank() && job.getModelAssetId() == null) {
                String assetKey = buildGeneratedAssetKey(recipe);
                ModelAsset asset = saveGeneratedAsset(recipe, assetKey, modelUrl, PROVIDER_FAIRSTACK);
                job.setModelAssetId(asset.getId());
                job.setStatus(AppConstants.STATUS_READY);
                recipe.setStatus(AppConstants.STATUS_READY);
                recipeRepository.save(recipe);
            }

            return generationJobRepository.save(job);
        } catch (Exception exception) {
            job.setErrorMessage(cleanError(exception.getMessage()));
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

    private ModelAsset saveGeneratedAsset(Recipe recipe, String assetKey, String remoteModelUrl, String provider) {
        try {
            byte[] glbBytes = PROVIDER_TRIPO.equals(provider)
                    ? tripoClient.downloadBinary(remoteModelUrl)
                    : fairStackClient.downloadBinary(remoteModelUrl);

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
            asset.setProvider(provider);
            asset.setStatus(AppConstants.STATUS_READY);
            asset.setQualityScore(PROVIDER_TRIPO.equals(provider) ? 0.74 : 0.60);
            asset.setTags(buildTags(recipe, provider));
            asset.setColorMode("FIXED");
            asset.setMaterialSlotsJson("{}");

            return modelAssetRepository.save(asset);
        } catch (Exception exception) {
            throw new BadRequestException(provider + " generó respuesta, pero no se pudo guardar el GLB: " + exception.getMessage());
        }
    }

    private String buildExternalPrompt(Recipe recipe) {
        String prompt = recipe.getModelPrompt();

        if (prompt == null || prompt.isBlank()) {
            prompt = recipe.getPrompt();
        }

        if (prompt == null || prompt.isBlank()) {
            throw new BadRequestException("La receta no tiene prompt para generar modelo 3D");
        }

        return prompt + " Single centered 3D object, isolated dessert asset, no plate, no table, no hands, no people, no text, no logo, clean geometry, game-ready GLB.";
    }

    private String buildGeneratedAssetKey(Recipe recipe) {
        String dessertType = recipe.getDessertType() == null || recipe.getDessertType().isBlank()
                ? "dessert"
                : recipe.getDessertType();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        return sanitize("generated_" + dessertType + "_recipe_" + recipe.getId() + "_" + timestamp);
    }

    private String buildTags(Recipe recipe, String provider) {
        StringBuilder builder = new StringBuilder();
        builder.append("generated,ai,complete_dessert,").append(provider.toLowerCase());

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

        return status.toString();
    }

    private String normalizeProviderStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized = status.trim().toUpperCase();

        if (normalized.contains("SUCCESS") || normalized.contains("FINISHED") || normalized.contains("COMPLETED") || normalized.contains("READY")) {
            return AppConstants.STATUS_READY;
        }

        if (normalized.contains("FAIL") || normalized.contains("ERROR") || normalized.contains("BANNED") || normalized.contains("EXPIRED") || normalized.contains("CANCELLED")) {
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

    private String cleanError(String message) {
        if (message == null || message.isBlank()) {
            return "El proveedor externo no devolvió detalle del error.";
        }

        return message.replace("\\n", "\n").replace("\\\"", "\"").trim();
    }
}