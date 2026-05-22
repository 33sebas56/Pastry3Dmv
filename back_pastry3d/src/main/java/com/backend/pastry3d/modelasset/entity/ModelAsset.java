package com.backend.pastry3d.modelasset.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_assets")
public class ModelAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_key", nullable = false, unique = true, length = 255)
    private String assetKey;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "dessert_type", length = 100)
    private String dessertType;

    @Column(name = "model_url", nullable = false, length = 1000)
    private String modelUrl;

    @Column(name = "preview_image_url", length = 1000)
    private String previewImageUrl;

    @Column(name = "provider", nullable = false, length = 100)
    private String provider;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "color_mode", length = 50)
    private String colorMode;

    @Column(name = "material_slots_json", columnDefinition = "TEXT")
    private String materialSlotsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ModelAsset() {}

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = "READY";
        if (provider == null) provider = "LOCAL_ASSET";
        if (qualityScore == null) qualityScore = 0.5;
        if (colorMode == null) colorMode = "FIXED";
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
