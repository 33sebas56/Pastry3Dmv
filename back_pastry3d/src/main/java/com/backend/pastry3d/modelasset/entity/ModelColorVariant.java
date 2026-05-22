package com.backend.pastry3d.modelasset.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "model_color_variants")
public class ModelColorVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_asset_id", nullable = false)
    private Long modelAssetId;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "color_name", nullable = false, length = 100)
    private String colorName;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(name = "is_default", nullable = false)
    private boolean defaultColor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getModelAssetId() { return modelAssetId; }
    public void setModelAssetId(Long modelAssetId) { this.modelAssetId = modelAssetId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public boolean isDefaultColor() { return defaultColor; }
    public void setDefaultColor(boolean defaultColor) { this.defaultColor = defaultColor; }
}
