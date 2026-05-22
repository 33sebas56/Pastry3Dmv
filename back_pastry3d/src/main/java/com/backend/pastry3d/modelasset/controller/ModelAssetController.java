package com.backend.pastry3d.modelasset.controller;

import com.backend.pastry3d.modelasset.dto.ModelAssetRequest;
import com.backend.pastry3d.modelasset.entity.ModelAsset;
import com.backend.pastry3d.modelasset.service.ModelAssetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelAssetController {
    private final ModelAssetService modelAssetService;

    public ModelAssetController(ModelAssetService modelAssetService) {
        this.modelAssetService = modelAssetService;
    }

    @GetMapping
    public List<ModelAsset> list(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String dessertType) {
        return modelAssetService.findAll(category, dessertType);
    }

    @GetMapping("/{id}")
    public ModelAsset get(@PathVariable Long id) {
        return modelAssetService.findById(id);
    }

    @PostMapping("/admin")
    public ModelAsset create(@Valid @RequestBody ModelAssetRequest request) {
        return modelAssetService.create(request);
    }

    @PutMapping("/admin/{id}")
    public ModelAsset update(@PathVariable Long id, @Valid @RequestBody ModelAssetRequest request) {
        return modelAssetService.update(id, request);
    }

    @PostMapping("/admin/import")
    public ModelAsset importGlb(@RequestParam String assetKey,
                                @RequestParam String name,
                                @RequestParam String category,
                                @RequestParam(required = false) String dessertType,
                                @RequestParam MultipartFile file) {
        return modelAssetService.importGlb(assetKey, name, category, dessertType, file);
    }

    @DeleteMapping("/admin/{id}")
    public void delete(@PathVariable Long id) {
        modelAssetService.delete(id);
    }
}
