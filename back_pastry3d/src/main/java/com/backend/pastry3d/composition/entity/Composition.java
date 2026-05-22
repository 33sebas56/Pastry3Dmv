package com.backend.pastry3d.composition.entity;

import jakarta.persistence.*;

@Entity
@Table(name="compositions")
public class Composition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long recipeId;
    @Column(columnDefinition="TEXT")
    private String sceneJson;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public String getSceneJson() { return sceneJson; }
    public void setSceneJson(String sceneJson) { this.sceneJson = sceneJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}