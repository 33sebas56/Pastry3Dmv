package com.backend.pastry3d.profile.service;

import com.backend.pastry3d.auth.entity.User;
import com.backend.pastry3d.profile.dto.ProfileUpdateRequest;
import com.backend.pastry3d.profile.entity.Profile;
import com.backend.pastry3d.profile.repository.ProfileRepository;
import com.backend.pastry3d.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional
    public Profile createDefaultProfile(User user, String displayName) {
        Profile profile = new Profile();
        profile.setUserId(user.getId());
        profile.setDisplayName(displayName);
        profile.setSkillLevel("BEGINNER");
        profile.setPreferencesJson("{}");
        return profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public Profile getByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));
    }

    @Transactional
    public Profile updateMyProfile(Long userId, ProfileUpdateRequest request) {
        Profile profile = getByUserId(userId);
        profile.setDisplayName(request.getDisplayName());
        profile.setAvatarUrl(request.getAvatarUrl());
        profile.setSkillLevel(request.getSkillLevel());
        profile.setPreferencesJson(request.getPreferencesJson());
        return profileRepository.save(profile);
    }
}
