package com.backend.pastry3d.modelasset.repository;

import com.backend.pastry3d.modelasset.entity.ModelColorVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelColorVariantRepository extends JpaRepository<ModelColorVariant, Long> {
    List<ModelColorVariant> findByModelAssetId(Long modelAssetId);
}
