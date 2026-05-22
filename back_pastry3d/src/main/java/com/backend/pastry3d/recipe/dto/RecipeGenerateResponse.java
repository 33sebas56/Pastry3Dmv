package com.backend.pastry3d.recipe.dto;

public class RecipeGenerateResponse {
    private Long recipeId;
    private String title;
    private String status;
    private String strategy;
    private String modelUrl;
    private String sceneJson;
    private String recipeJson;
    private String visualPlanJson;
    private String modelPrompt;
    private String missingAssetsJson;
    private Long generationJobId;

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getModelUrl() { return modelUrl; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }
    public String getSceneJson() { return sceneJson; }
    public void setSceneJson(String sceneJson) { this.sceneJson = sceneJson; }
    public String getRecipeJson() { return recipeJson; }
    public void setRecipeJson(String recipeJson) { this.recipeJson = recipeJson; }
    public String getVisualPlanJson() { return visualPlanJson; }
    public void setVisualPlanJson(String visualPlanJson) { this.visualPlanJson = visualPlanJson; }
    public String getModelPrompt() { return modelPrompt; }
    public void setModelPrompt(String modelPrompt) { this.modelPrompt = modelPrompt; }
    public String getMissingAssetsJson() { return missingAssetsJson; }
    public void setMissingAssetsJson(String missingAssetsJson) { this.missingAssetsJson = missingAssetsJson; }
    public Long getGenerationJobId() { return generationJobId; }
    public void setGenerationJobId(Long generationJobId) { this.generationJobId = generationJobId; }
}
