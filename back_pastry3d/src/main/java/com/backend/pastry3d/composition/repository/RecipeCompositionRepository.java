package com.backend.pastry3d.composition.repository;

import com.backend.pastry3d.composition.entity.RecipeComposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeCompositionRepository extends JpaRepository<RecipeComposition, Long> {
    Optional<RecipeComposition> findTopByRecipeIdOrderByCreatedAtDesc(Long recipeId);
    List<RecipeComposition> findByRecipeIdOrderByCreatedAtDesc(Long recipeId);
}
