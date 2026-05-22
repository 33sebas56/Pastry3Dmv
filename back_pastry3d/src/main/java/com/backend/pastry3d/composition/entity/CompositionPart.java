package com.backend.pastry3d.composition.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "composition_parts")
public class CompositionPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "composition_id", nullable = false)
    private Long compositionId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "anchor", length = 100)
    private String anchor;

    @Column(name = "position_json", columnDefinition = "TEXT")
    private String positionJson;

    @Column(name = "scale")
    private Double scale;

    @Column(name = "rotation_json", columnDefinition = "TEXT")
    private String rotationJson;

    @Column(name = "color_overrides_json", columnDefinition = "TEXT")
    private String colorOverridesJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompositionId() { return compositionId; }
    public void setCompositionId(Long compositionId) { this.compositionId = compositionId; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public String getAnchor() { return anchor; }
    public void setAnchor(String anchor) { this.anchor = anchor; }
    public String getPositionJson() { return positionJson; }
    public void setPositionJson(String positionJson) { this.positionJson = positionJson; }
    public Double getScale() { return scale; }
    public void setScale(Double scale) { this.scale = scale; }
    public String getRotationJson() { return rotationJson; }
    public void setRotationJson(String rotationJson) { this.rotationJson = rotationJson; }
    public String getColorOverridesJson() { return colorOverridesJson; }
    public void setColorOverridesJson(String colorOverridesJson) { this.colorOverridesJson = colorOverridesJson; }
}
