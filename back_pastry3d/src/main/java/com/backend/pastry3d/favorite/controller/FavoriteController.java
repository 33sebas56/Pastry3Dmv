package com.backend.pastry3d.favorite.controller;

import com.backend.pastry3d.auth.entity.User;
import com.backend.pastry3d.favorite.dto.FavoriteRequest;
import com.backend.pastry3d.favorite.entity.Favorite;
import com.backend.pastry3d.favorite.service.FavoriteService;
import com.backend.pastry3d.shared.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;
    private final CurrentUserService currentUserService;

    public FavoriteController(FavoriteService favoriteService, CurrentUserService currentUserService) {
        this.favoriteService = favoriteService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public Favorite create(@Valid @RequestBody FavoriteRequest request, Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return favoriteService.create(currentUser.getId(), request);
    }

    @GetMapping("/me")
    public List<Favorite> findMine(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return favoriteService.findMine(currentUser.getId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        favoriteService.delete(id, currentUser.getId());
    }
}
