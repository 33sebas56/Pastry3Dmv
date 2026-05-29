package com.backend.pastry3d.generation.repository;

import com.backend.pastry3d.generation.entity.GenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, Long> {
    List<GenerationJob> findByRecipeIdOrderByCreatedAtDesc(Long recipeId);

    List<GenerationJob> findByRecipeIdAndProviderAndStatusInOrderByCreatedAtDesc(Long recipeId, String provider, List<String> statuses);
}
