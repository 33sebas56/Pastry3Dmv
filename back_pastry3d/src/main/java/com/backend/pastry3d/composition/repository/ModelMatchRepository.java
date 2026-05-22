package com.backend.pastry3d.composition.repository;

import com.backend.pastry3d.composition.entity.ModelMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelMatchRepository extends JpaRepository<ModelMatch, Long> {
}
