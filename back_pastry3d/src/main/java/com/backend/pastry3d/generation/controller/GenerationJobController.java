package com.backend.pastry3d.generation.controller;

import com.backend.pastry3d.auth.entity.User;
import com.backend.pastry3d.generation.entity.GenerationJob;
import com.backend.pastry3d.generation.service.GenerationJobService;
import com.backend.pastry3d.shared.security.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generation-jobs")
public class GenerationJobController {

    private final GenerationJobService generationJobService;
    private final CurrentUserService currentUserService;

    public GenerationJobController(
            GenerationJobService generationJobService,
            CurrentUserService currentUserService
    ) {
        this.generationJobService = generationJobService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{id}")
    public GenerationJob getJob(@PathVariable Long id) {
        return generationJobService.getJob(id);
    }

    @PostMapping("/recipes/{recipeId}/start-tripo")
    public GenerationJob startTripoForRecipe(
            @PathVariable Long recipeId,
            Authentication authentication
    ) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return generationJobService.startTripoForRecipe(recipeId, currentUser.getId());
    }

    @PostMapping("/recipes/{recipeId}/start-fairstack")
    public GenerationJob startFairStackForRecipe(
            @PathVariable Long recipeId,
            Authentication authentication
    ) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return generationJobService.startFairStackForRecipe(recipeId, currentUser.getId());
    }

    @PostMapping("/{id}/sync")
    public GenerationJob syncJob(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return generationJobService.syncJob(id, currentUser.getId());
    }
}