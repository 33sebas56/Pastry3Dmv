package com.backend.pastry3d.profile.controller;

import com.backend.pastry3d.auth.entity.User;
import com.backend.pastry3d.profile.dto.ProfileUpdateRequest;
import com.backend.pastry3d.profile.entity.Profile;
import com.backend.pastry3d.profile.service.ProfileService;
import com.backend.pastry3d.shared.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    public ProfileController(ProfileService profileService, CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Profile getMyProfile(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return profileService.getByUserId(currentUser.getId());
    }

    @PutMapping
    public Profile updateMyProfile(@Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return profileService.updateMyProfile(currentUser.getId(), request);
    }
}
