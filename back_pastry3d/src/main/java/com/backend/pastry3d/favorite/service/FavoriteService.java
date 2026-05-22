package com.backend.pastry3d.favorite.service;

import com.backend.pastry3d.favorite.dto.FavoriteRequest;
import com.backend.pastry3d.favorite.entity.Favorite;
import com.backend.pastry3d.favorite.repository.FavoriteRepository;
import com.backend.pastry3d.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @Transactional
    public Favorite create(Long userId, FavoriteRequest request) {
        return favoriteRepository.findByUserIdAndTargetIdAndTargetType(userId, request.getTargetId(), request.getTargetType())
                .orElseGet(() -> {
                    Favorite favorite = new Favorite();
                    favorite.setUserId(userId);
                    favorite.setTargetId(request.getTargetId());
                    favorite.setTargetType(request.getTargetType());
                    return favoriteRepository.save(favorite);
                });
    }

    @Transactional(readOnly = true)
    public List<Favorite> findMine(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Favorite favorite = favoriteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Favorito no encontrado"));
        if (!favorite.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Favorito no encontrado");
        }
        favoriteRepository.delete(favorite);
    }
}
