package com.backend.pastry3d.generation.service;

import com.backend.pastry3d.adapters.fairstack.FairStackClient;
import com.backend.pastry3d.generation.entity.GenerationJob;
import com.backend.pastry3d.generation.repository.GenerationJobRepository;
import com.backend.pastry3d.shared.constants.AppConstants;
import com.backend.pastry3d.shared.exception.BadRequestException;
import com.backend.pastry3d.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class GenerationJobService {
    private final GenerationJobRepository generationJobRepository;
    private final FairStackClient fairStackClient;
    private final ObjectMapper objectMapper;
    private final boolean fairStackEnabled;

    public GenerationJobService(
            GenerationJobRepository generationJobRepository,
            FairStackClient fairStackClient,
            ObjectMapper objectMapper,
            @Value("${fairstack.enabled:false}") boolean fairStackEnabled
    ) {
        this.generationJobRepository = generationJobRepository;
        this.fairStackClient = fairStackClient;
        this.objectMapper = objectMapper;
        this.fairStackEnabled = fairStackEnabled;
    }

    @Transactional
    public GenerationJob createPendingManualJob(Long recipeId, String modelPrompt) {
        GenerationJob job = new GenerationJob();
        job.setRecipeId(recipeId);
        job.setProvider("MANUAL_IMPORT");
        job.setStatus(AppConstants.STATUS_PENDING_MANUAL_MODEL);
        job.setRequestJson(safeJson(Map.of("modelPrompt", modelPrompt)));
        return generationJobRepository.save(job);
    }

    @Transactional
    public GenerationJob startFairStackJob(Long recipeId, String modelPrompt) {
        if (!fairStackEnabled) {
            throw new BadRequestException("FairStack está desactivado. La receta debe quedar como PENDING_MANUAL_MODEL.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", modelPrompt);
        payload.put("recipeId", recipeId);

        GenerationJob job = new GenerationJob();
        job.setRecipeId(recipeId);
        job.setProvider("FAIRSTACK");
        job.setStatus(AppConstants.STATUS_PENDING);
        job.setRequestJson(safeJson(payload));
        GenerationJob saved = generationJobRepository.save(job);

        try {
            Map<String, Object> response = fairStackClient.startGeneration(payload);
            saved.setResponseJson(safeJson(response));
            Object providerJobId = response.get("id") != null ? response.get("id") : response.get("jobId");
            if (providerJobId != null) {
                saved.setProviderJobId(providerJobId.toString());
            }
            saved.setStatus(AppConstants.STATUS_GENERATING);
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
    public GenerationJob syncJob(Long id) {
        GenerationJob job = getJob(id);
        if (!"FAIRSTACK".equals(job.getProvider())) {
            return job;
        }
        if (job.getProviderJobId() == null || job.getProviderJobId().isBlank()) {
            throw new BadRequestException("El job no tiene providerJobId");
        }
        try {
            Map<String, Object> response = fairStackClient.getJobStatus(job.getProviderJobId());
            job.setResponseJson(safeJson(response));
            Object status = response.get("status");
            if (status != null) {
                job.setStatus(status.toString().toUpperCase());
            }
            return generationJobRepository.save(job);
        } catch (Exception exception) {
            job.setErrorMessage(exception.getMessage());
            return generationJobRepository.save(job);
        }
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
