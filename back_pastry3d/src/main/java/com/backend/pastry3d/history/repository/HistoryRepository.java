package com.backend.pastry3d.history.repository;

import com.backend.pastry3d.history.entity.History;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<History> findByRecipeIdOrderByCreatedAtDesc(Long recipeId);
}
