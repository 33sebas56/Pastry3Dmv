package com.backend.pastry3d.modelasset.dto;

import jakarta.validation.constraints.NotBlank;

public class ModelAssetRequest {
    @NotBlank(message = "assetKey es obligatorio")
    private String assetKey;
    @NotBlank(message = "name es obligatorio")
    private String name;
    @NotBlank(message = "category es obligatorio")
    private String category;
    private String dessertType;
    @NotBlank(message = "modelUrl es obligatorio")
    private String modelUrl;
    private String previewImageUrl;
    private String provider;
    private String status;
    private Double qualityScore;
    private String tags;
    private String colorMode;
    private String materialSlotsJson;

    public String getAssetKey() { return assetKey; }
    public void setAssetKey(String assetKey) { this.assetKey = assetKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDessertType() { return dessertType; }
    public void setDessertType(String dessertType) { this.dessertType = dessertType; }
    public String getModelUrl() { return modelUrl; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }
    public String getPreviewImageUrl() { return previewImageUrl; }
    public void setPreviewImageUrl(String previewImageUrl) { this.previewImageUrl = previewImageUrl; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getColorMode() { return colorMode; }
    public void setColorMode(String colorMode) { this.colorMode = colorMode; }
    public String getMaterialSlotsJson() { return materialSlotsJson; }
    public void setMaterialSlotsJson(String materialSlotsJson) { this.materialSlotsJson = materialSlotsJson; }
}
