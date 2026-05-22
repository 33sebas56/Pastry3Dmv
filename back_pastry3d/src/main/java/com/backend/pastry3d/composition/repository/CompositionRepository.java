package com.backend.pastry3d.composition.repository;

import com.backend.pastry3d.composition.entity.Composition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompositionRepository extends JpaRepository<Composition, Long> {
    List<Composition> findByRecipeId(Long recipeId);
}