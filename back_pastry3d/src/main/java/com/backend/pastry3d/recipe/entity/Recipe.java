package com.backend.pastry3d.recipe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipes")
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "normalized_prompt", columnDefinition = "TEXT")
    private String normalizedPrompt;

    @Column(name = "recipe_json", columnDefinition = "TEXT")
    private String recipeJson;

    @Column(name = "visual_plan_json", columnDefinition = "TEXT")
    private String visualPlanJson;

    @Column(name = "dessert_type", length = 100)
    private String dessertType;

    @Column(name = "difficulty", length = 50)
    private String difficulty;

    @Column(name = "servings")
    private Integer servings;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "model_prompt", columnDefinition = "TEXT")
    private String modelPrompt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Recipe() {}

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = "PENDING";
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getNormalizedPrompt() { return normalizedPrompt; }
    public void setNormalizedPrompt(String normalizedPrompt) { this.normalizedPrompt = normalizedPrompt; }
    public String getRecipeJson() { return recipeJson; }
    public void setRecipeJson(String recipeJson) { this.recipeJson = recipeJson; }
    public String getVisualPlanJson() { return visualPlanJson; }
    public void setVisualPlanJson(String visualPlanJson) { this.visualPlanJson = visualPlanJson; }
    public String getDessertType() { return dessertType; }
    public void setDessertType(String dessertType) { this.dessertType = dessertType; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModelPrompt() { return modelPrompt; }
    public void setModelPrompt(String modelPrompt) { this.modelPrompt = modelPrompt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
