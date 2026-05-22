package com.backend.pastry3d.recipe.controller;

import com.backend.pastry3d.auth.entity.User;
import com.backend.pastry3d.recipe.dto.RecipeDetailResponse;
import com.backend.pastry3d.recipe.dto.RecipeGenerateRequest;
import com.backend.pastry3d.recipe.dto.RecipeGenerateResponse;
import com.backend.pastry3d.recipe.entity.Recipe;
import com.backend.pastry3d.recipe.service.RecipeService;
import com.backend.pastry3d.shared.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private final RecipeService recipeService;
    private final CurrentUserService currentUserService;

    public RecipeController(RecipeService recipeService, CurrentUserService currentUserService) {
        this.recipeService = recipeService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/generate")
    public RecipeGenerateResponse generate(@Valid @RequestBody RecipeGenerateRequest request, Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return recipeService.generateRecipe(request.getPrompt(), currentUser);
    }

    @GetMapping
    public List<Recipe> listMine(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return recipeService.findMine(currentUser.getId());
    }

    @GetMapping("/{id}")
    public RecipeDetailResponse get(@PathVariable Long id, Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return recipeService.getDetail(id, currentUser.getId());
    }

    @PostMapping("/{id}/build-composition")
    public RecipeGenerateResponse rebuildComposition(@PathVariable Long id, Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return recipeService.rebuildComposition(id, currentUser.getId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        recipeService.delete(id, currentUser.getId());
    }
}
