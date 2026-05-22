package com.backend.pastry3d.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FavoriteRequest {
    @NotNull(message = "targetId es obligatorio")
    private Long targetId;
    @NotBlank(message = "targetType es obligatorio")
    private String targetType;

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
}
