package com.backend.pastry3d.profile.dto;

import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {
    @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
    private String displayName;

    @Size(max = 500, message = "La URL del avatar no puede superar 500 caracteres")
    private String avatarUrl;

    @Size(max = 40, message = "El nivel no puede superar 40 caracteres")
    private String skillLevel;

    @Size(max = 2000, message = "Las preferencias no pueden superar 2000 caracteres")
    private String preferencesJson;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }
    public String getPreferencesJson() { return preferencesJson; }
    public void setPreferencesJson(String preferencesJson) { this.preferencesJson = preferencesJson; }
}
