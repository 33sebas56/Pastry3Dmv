INSERT INTO model_assets (
    asset_key, name, category, dessert_type, model_url, provider, status, quality_score, tags, color_mode, material_slots_json
) VALUES
(
    'round_cake_base_vanilla',
    'Base de pastel redondo vainilla',
    'BASE',
    'round_cake',
    '/uploads/models/demo/round_cake_base_vanilla.glb',
    'LOCAL_ASSET',
    'READY',
    0.95,
    'cake,pastel,base,vanilla,vainilla,round_cake',
    'MULTI_MATERIAL',
    '{"frosting":{"recolorable":true,"default":"#FFF2CC"},"cake":{"recolorable":true,"default":"#D8A15D"}}'
),
(
    'caramel_rose',
    'Rosa de caramelo recoloreable',
    'TOPPING',
    'round_cake',
    '/uploads/models/demo/caramel_rose.glb',
    'LOCAL_ASSET',
    'READY',
    0.92,
    'rose,rosa,caramel,caramelo,topping,decoration',
    'MULTI_MATERIAL',
    '{"petals":{"recolorable":true,"default":"#C92A3A"},"primary":{"recolorable":true,"default":"#C92A3A"}}'
),
(
    'strawberry_topping',
    'Fresa decorativa',
    'TOPPING',
    'round_cake',
    '/uploads/models/demo/strawberry_topping.glb',
    'LOCAL_ASSET',
    'READY',
    0.80,
    'strawberry,fresa,topping,fruit,fruta',
    'FIXED',
    '{}'
),
(
    'birthday_candle',
    'Vela de cumpleaños',
    'DECORATION',
    'round_cake',
    '/uploads/models/demo/birthday_candle.glb',
    'LOCAL_ASSET',
    'READY',
    0.78,
    'candle,vela,birthday,cumpleaños,decoration',
    'MULTI_MATERIAL',
    '{"primary":{"recolorable":true,"default":"#FFD166"}}'
)
ON CONFLICT (asset_key) DO NOTHING;

INSERT INTO model_color_variants (model_asset_id, material_name, color_name, color_hex, is_default)
SELECT id, 'petals', 'rojo', '#C92A3A', TRUE FROM model_assets WHERE asset_key = 'caramel_rose'
ON CONFLICT DO NOTHING;

INSERT INTO model_color_variants (model_asset_id, material_name, color_name, color_hex, is_default)
SELECT id, 'petals', 'azul', '#2F80ED', FALSE FROM model_assets WHERE asset_key = 'caramel_rose'
ON CONFLICT DO NOTHING;
