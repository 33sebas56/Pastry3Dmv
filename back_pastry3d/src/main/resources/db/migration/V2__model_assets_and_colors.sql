CREATE TABLE model_assets (
    id BIGSERIAL PRIMARY KEY,
    asset_key VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    dessert_type VARCHAR(100),
    model_url VARCHAR(1000) NOT NULL,
    preview_image_url VARCHAR(1000),
    provider VARCHAR(100) NOT NULL DEFAULT 'LOCAL_ASSET',
    status VARCHAR(50) NOT NULL DEFAULT 'READY',
    quality_score DOUBLE PRECISION DEFAULT 0.5,
    tags TEXT,
    color_mode VARCHAR(50) DEFAULT 'FIXED',
    material_slots_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_model_assets_category_status ON model_assets(category, status);
CREATE INDEX idx_model_assets_dessert_category_status ON model_assets(dessert_type, category, status);

CREATE TABLE model_color_variants (
    id BIGSERIAL PRIMARY KEY,
    model_asset_id BIGINT NOT NULL REFERENCES model_assets(id) ON DELETE CASCADE,
    material_name VARCHAR(100) NOT NULL,
    color_name VARCHAR(100) NOT NULL,
    color_hex VARCHAR(7) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);
