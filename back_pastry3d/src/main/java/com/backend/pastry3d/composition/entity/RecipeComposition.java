package com.backend.pastry3d.composition.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipe_compositions")
public class RecipeComposition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "strategy", nullable = false, length = 100)
    private String strategy;

    @Column(name = "base_asset_id")
    private Long baseAssetId;

    @Column(name = "complete_model_asset_id")
    private Long completeModelAssetId;

    @Column(name = "scene_json", columnDefinition = "TEXT")
    private String sceneJson;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "missing_assets_json", columnDefinition = "TEXT")
    private String missingAssetsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RecipeComposition() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public Long getBaseAssetId() { return baseAssetId; }
    public void setBaseAssetId(Long baseAssetId) { this.baseAssetId = baseAssetId; }
    public Long getCompleteModelAssetId() { return completeModelAssetId; }
    public void setCompleteModelAssetId(Long completeModelAssetId) { this.completeModelAssetId = completeModelAssetId; }
    public String getSceneJson() { return sceneJson; }
    public void setSceneJson(String sceneJson) { this.sceneJson = sceneJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMissingAssetsJson() { return missingAssetsJson; }
    public void setMissingAssetsJson(String missingAssetsJson) { this.missingAssetsJson = missingAssetsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
