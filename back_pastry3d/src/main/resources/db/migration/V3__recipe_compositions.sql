CREATE TABLE recipe_compositions (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    strategy VARCHAR(100) NOT NULL,
    base_asset_id BIGINT REFERENCES model_assets(id),
    complete_model_asset_id BIGINT REFERENCES model_assets(id),
    scene_json TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    missing_assets_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recipe_compositions_recipe ON recipe_compositions(recipe_id, created_at DESC);

CREATE TABLE composition_parts (
    id BIGSERIAL PRIMARY KEY,
    composition_id BIGINT NOT NULL REFERENCES recipe_compositions(id) ON DELETE CASCADE,
    asset_id BIGINT NOT NULL REFERENCES model_assets(id),
    anchor VARCHAR(100),
    position_json TEXT,
    scale DOUBLE PRECISION,
    rotation_json TEXT,
    color_overrides_json TEXT
);

CREATE TABLE model_matches (
    id BIGSERIAL PRIMARY KEY,
    recipe_id BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    model_asset_id BIGINT NOT NULL REFERENCES model_assets(id),
    score DOUBLE PRECISION,
    decision VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
