package com.backend.pastry3d.composition.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_matches")
public class ModelMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "model_asset_id", nullable = false)
    private Long modelAssetId;

    @Column(name = "score")
    private Double score;

    @Column(name = "decision", length = 100)
    private String decision;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public Long getModelAssetId() { return modelAssetId; }
    public void setModelAssetId(Long modelAssetId) { this.modelAssetId = modelAssetId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
