package com.backend.pastry3d.modelasset.service;

import com.backend.pastry3d.modelasset.dto.ModelAssetRequest;
import com.backend.pastry3d.modelasset.entity.ModelAsset;
import com.backend.pastry3d.modelasset.repository.ModelAssetRepository;
import com.backend.pastry3d.shared.constants.AppConstants;
import com.backend.pastry3d.shared.exception.BadRequestException;
import com.backend.pastry3d.shared.exception.ResourceNotFoundException;
import com.backend.pastry3d.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ModelAssetService {
    private final ModelAssetRepository modelAssetRepository;
    private final FileStorageService fileStorageService;

    public ModelAssetService(ModelAssetRepository modelAssetRepository, FileStorageService fileStorageService) {
        this.modelAssetRepository = modelAssetRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<ModelAsset> findAll(String category, String dessertType) {
        if (dessertType != null && category != null) {
            return modelAssetRepository.findByDessertTypeAndCategoryAndStatusOrderByQualityScoreDesc(
                    dessertType, category, AppConstants.STATUS_READY
            );
        }
        if (category != null) {
            return modelAssetRepository.findByCategoryAndStatusOrderByQualityScoreDesc(category, AppConstants.STATUS_READY);
        }
        return modelAssetRepository.findByStatusOrderByQualityScoreDesc(AppConstants.STATUS_READY);
    }

    @Transactional(readOnly = true)
    public ModelAsset findById(Long id) {
        return modelAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo 3D no encontrado"));
    }

    @Transactional
    public ModelAsset create(ModelAssetRequest request) {
        if (modelAssetRepository.existsByAssetKey(request.getAssetKey())) {
            throw new BadRequestException("Ya existe un modelo con ese assetKey");
        }
        ModelAsset asset = mapRequest(new ModelAsset(), request);
        return modelAssetRepository.save(asset);
    }

    @Transactional
    public ModelAsset update(Long id, ModelAssetRequest request) {
        ModelAsset asset = findById(id);
        if (!asset.getAssetKey().equals(request.getAssetKey()) && modelAssetRepository.existsByAssetKey(request.getAssetKey())) {
            throw new BadRequestException("Ya existe un modelo con ese assetKey");
        }
        return modelAssetRepository.save(mapRequest(asset, request));
    }

    @Transactional
    public ModelAsset importGlb(String assetKey, String name, String category, String dessertType, MultipartFile file) {
        if (modelAssetRepository.existsByAssetKey(assetKey)) {
            throw new BadRequestException("Ya existe un modelo con ese assetKey");
        }
        String modelUrl = fileStorageService.saveModelFile(assetKey, file);
        ModelAsset asset = new ModelAsset();
        asset.setAssetKey(assetKey);
        asset.setName(name);
        asset.setCategory(category);
        asset.setDessertType(dessertType);
        asset.setModelUrl(modelUrl);
        asset.setProvider("IMPORTED_GLB");
        asset.setStatus(AppConstants.STATUS_READY);
        asset.setQualityScore(0.8);
        asset.setColorMode("FIXED");
        return modelAssetRepository.save(asset);
    }

    @Transactional
    public void delete(Long id) {
        ModelAsset asset = findById(id);
        modelAssetRepository.delete(asset);
    }

    private ModelAsset mapRequest(ModelAsset asset, ModelAssetRequest request) {
        asset.setAssetKey(request.getAssetKey());
        asset.setName(request.getName());
        asset.setCategory(request.getCategory());
        asset.setDessertType(request.getDessertType());
        asset.setModelUrl(request.getModelUrl());
        asset.setPreviewImageUrl(request.getPreviewImageUrl());
        asset.setProvider(request.getProvider() == null || request.getProvider().isBlank() ? "LOCAL_ASSET" : request.getProvider());
        asset.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? AppConstants.STATUS_READY : request.getStatus());
        asset.setQualityScore(request.getQualityScore() == null ? 0.5 : request.getQualityScore());
        asset.setTags(request.getTags());
        asset.setColorMode(request.getColorMode() == null ? "FIXED" : request.getColorMode());
        asset.setMaterialSlotsJson(request.getMaterialSlotsJson());
        return asset;
    }
}
