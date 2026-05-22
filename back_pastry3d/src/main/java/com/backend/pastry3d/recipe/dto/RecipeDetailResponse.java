package com.backend.pastry3d.recipe.dto;

public class RecipeDetailResponse {
    private Long id;
    private String title;
    private String prompt;
    private String dessertType;
    private String difficulty;
    private Integer servings;
    private String status;
    private String recipeJson;
    private String visualPlanJson;
    private String modelPrompt;
    private String strategy;
    private String modelUrl;
    private String sceneJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getDessertType() { return dessertType; }
    public void setDessertType(String dessertType) { this.dessertType = dessertType; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRecipeJson() { return recipeJson; }
    public void setRecipeJson(String recipeJson) { this.recipeJson = recipeJson; }
    public String getVisualPlanJson() { return visualPlanJson; }
    public void setVisualPlanJson(String visualPlanJson) { this.visualPlanJson = visualPlanJson; }
    public String getModelPrompt() { return modelPrompt; }
    public void setModelPrompt(String modelPrompt) { this.modelPrompt = modelPrompt; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getModelUrl() { return modelUrl; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }
    public String getSceneJson() { return sceneJson; }
    public void setSceneJson(String sceneJson) { this.sceneJson = sceneJson; }
}
