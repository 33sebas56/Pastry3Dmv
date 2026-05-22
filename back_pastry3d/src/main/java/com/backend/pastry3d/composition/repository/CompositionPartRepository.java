package com.backend.pastry3d.composition.repository;

import com.backend.pastry3d.composition.entity.CompositionPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompositionPartRepository extends JpaRepository<CompositionPart, Long> {
    List<CompositionPart> findByCompositionId(Long compositionId);
}
