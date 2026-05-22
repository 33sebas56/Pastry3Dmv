package com.backend.pastry3d.modelasset.repository;

import com.backend.pastry3d.modelasset.entity.ModelAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModelAssetRepository extends JpaRepository<ModelAsset, Long> {
    Optional<ModelAsset> findByAssetKey(String assetKey);
    boolean existsByAssetKey(String assetKey);
    List<ModelAsset> findByStatusOrderByQualityScoreDesc(String status);
    List<ModelAsset> findByCategoryAndStatusOrderByQualityScoreDesc(String category, String status);
    List<ModelAsset> findByDessertTypeAndCategoryAndStatusOrderByQualityScoreDesc(String dessertType, String category, String status);
    Optional<ModelAsset> findFirstByDessertTypeAndCategoryAndStatusOrderByQualityScoreDesc(String dessertType, String category, String status);
}
