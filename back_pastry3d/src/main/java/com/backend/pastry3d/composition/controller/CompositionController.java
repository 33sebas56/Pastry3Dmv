package com.backend.pastry3d.composition.controller;

import com.backend.pastry3d.composition.entity.RecipeComposition;
import com.backend.pastry3d.composition.service.CompositionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compositions")
public class CompositionController {
    private final CompositionService compositionService;

    public CompositionController(CompositionService compositionService) {
        this.compositionService = compositionService;
    }

    @GetMapping("/recipe/{recipeId}")
    public RecipeComposition getLatestByRecipe(@PathVariable Long recipeId) {
        return compositionService.getLatestByRecipeId(recipeId);
    }
}
