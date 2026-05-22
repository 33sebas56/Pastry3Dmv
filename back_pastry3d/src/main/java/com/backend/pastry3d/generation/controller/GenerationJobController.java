package com.backend.pastry3d.generation.controller;

import com.backend.pastry3d.generation.entity.GenerationJob;
import com.backend.pastry3d.generation.service.GenerationJobService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generation-jobs")
public class GenerationJobController {
    private final GenerationJobService generationJobService;

    public GenerationJobController(GenerationJobService generationJobService) {
        this.generationJobService = generationJobService;
    }

    @GetMapping("/{id}")
    public GenerationJob getJob(@PathVariable Long id) {
        return generationJobService.getJob(id);
    }

    @PostMapping("/{id}/sync")
    public GenerationJob syncJob(@PathVariable Long id) {
        return generationJobService.syncJob(id);
    }
}
