package com.backend.pastry3d.recipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RecipeGenerateRequest {
    @NotBlank(message = "El prompt es obligatorio")
    @Size(min = 5, max = 1000, message = "El prompt debe tener entre 5 y 1000 caracteres")
    private String prompt;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
