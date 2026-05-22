UPDATE model_assets
SET
    name = 'Grupo de rosas de caramelo',
    model_url = '/uploads/models/demo/caramel_rose_cluster.glb',
    tags = 'rose,rosa,rosas,flower,flor,caramel,caramelo,topping,decoration,cluster',
    color_mode = 'MULTI_MATERIAL',
    material_slots_json = '{"petals":{"recolorable":true,"default":"#C92A3A"},"primary":{"recolorable":true,"default":"#C92A3A"}}',
    updated_at = CURRENT_TIMESTAMP
WHERE asset_key = 'caramel_rose';

INSERT INTO model_assets (
    asset_key,
    name,
    category,
    dessert_type,
    model_url,
    provider,
    status,
    quality_score,
    tags,
    color_mode,
    material_slots_json
) VALUES
(
    'milhojas_base_classic',
    'Base de milhojas clásica',
    'BASE',
    'milhojas',
    '/uploads/models/demo/milhojas_base_classic.glb',
    'LOCAL_ASSET',
    'READY',
    0.96,
    'milhojas,millefeuille,mille-feuille,hojaldre,capas,crema,pastry,base',
    'FIXED',
    '{}'
),
(
    'cupcake_base_vanilla',
    'Base de cupcake de vainilla',
    'BASE',
    'cupcake',
    '/uploads/models/demo/cupcake_base_vanilla.glb',
    'LOCAL_ASSET',
    'READY',
    0.88,
    'cupcake,magdalena,vainilla,vanilla,base,cream,frosting',
    'MULTI_MATERIAL',
    '{"cream":{"recolorable":true,"default":"#FFF2E6"},"wrapper":{"recolorable":true,"default":"#D8A15D"},"primary":{"recolorable":true,"default":"#FFF2E6"}}'
),
(
    'cheesecake_base_plain',
    'Base de cheesecake simple',
    'BASE',
    'cheesecake',
    '/uploads/models/demo/cheesecake_base_plain.glb',
    'LOCAL_ASSET',
    'READY',
    0.86,
    'cheesecake,pay de queso,tarta de queso,base,plain,simple',
    'FIXED',
    '{}'
),
(
    'tart_base_vanilla',
    'Base de tarta de vainilla',
    'BASE',
    'tart',
    '/uploads/models/demo/tart_base_vanilla.glb',
    'LOCAL_ASSET',
    'READY',
    0.84,
    'tart,tarta,pie,pay,base,vainilla,vanilla',
    'FIXED',
    '{}'
),
(
    'cream_swirl',
    'Remolino de crema',
    'TOPPING',
    NULL,
    '/uploads/models/demo/cream_swirl.glb',
    'LOCAL_ASSET',
    'READY',
    0.84,
    'cream,crema,nata,chantilly,swirl,copo,topping,decoration',
    'MULTI_MATERIAL',
    '{"cream":{"recolorable":true,"default":"#FFF2E6"},"primary":{"recolorable":true,"default":"#FFF2E6"}}'
),
(
    'chocolate_drizzle',
    'Chorreado de chocolate',
    'SAUCE',
    NULL,
    '/uploads/models/demo/chocolate_drizzle.glb',
    'LOCAL_ASSET',
    'READY',
    0.76,
    'chocolate,drizzle,salsa,baño,bano,cobertura,topping,sauce',
    'MULTI_MATERIAL',
    '{"chocolate":{"recolorable":true,"default":"#4B2418"},"primary":{"recolorable":true,"default":"#4B2418"}}'
),
(
    'macaron_topping',
    'Macaron decorativo',
    'TOPPING',
    NULL,
    '/uploads/models/demo/macaron_topping.glb',
    'LOCAL_ASSET',
    'READY',
    0.79,
    'macaron,macarron,macarrón,topping,decoration,pastel,color',
    'MULTI_MATERIAL',
    '{"primary":{"recolorable":true,"default":"#FFD6E7"},"shell":{"recolorable":true,"default":"#FFD6E7"}}'
),
(
    'chocolate_bar_piece',
    'Trozo de chocolate',
    'TOPPING',
    NULL,
    '/uploads/models/demo/chocolate_bar_piece.glb',
    'LOCAL_ASSET',
    'READY',
    0.82,
    'chocolate,bar,piece,trozo,pedazo,decoracion,decoration,topping',
    'MULTI_MATERIAL',
    '{"chocolate":{"recolorable":true,"default":"#4B2418"},"primary":{"recolorable":true,"default":"#4B2418"}}'
)
ON CONFLICT (asset_key) DO UPDATE SET
    name = EXCLUDED.name,
    category = EXCLUDED.category,
    dessert_type = EXCLUDED.dessert_type,
    model_url = EXCLUDED.model_url,
    provider = EXCLUDED.provider,
    status = EXCLUDED.status,
    quality_score = EXCLUDED.quality_score,
    tags = EXCLUDED.tags,
    color_mode = EXCLUDED.color_mode,
    material_slots_json = EXCLUDED.material_slots_json,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO model_color_variants (model_asset_id, material_name, color_name, color_hex, is_default)
SELECT id, 'petals', 'rojo', '#C92A3A', TRUE
FROM model_assets
WHERE asset_key = 'caramel_rose'
ON CONFLICT DO NOTHING;

INSERT INTO model_color_variants (model_asset_id, material_name, color_name, color_hex, is_default)
SELECT id, 'petals', 'azul', '#2F80ED', FALSE
FROM model_assets
WHERE asset_key = 'caramel_rose'
ON CONFLICT DO NOTHING;

INSERT INTO model_color_variants (model_asset_id, material_name, color_name, color_hex, is_default)
SELECT id, 'cream', 'blanco crema', '#FFF2E6', TRUE
FROM model_assets
WHERE asset_key = 'cream_swirl'
ON CONFLICT DO NOTHING;

INSERT INTO model_color_variants (model_asset_id, material_name, color_name, color_hex, is_default)
SELECT id, 'chocolate', 'chocolate', '#4B2418', TRUE
FROM model_assets
WHERE asset_key = 'chocolate_drizzle'
ON CONFLICT DO NOTHING;

INSERT INTO model_color_variants (model_asset_id, material_name, color_name, color_hex, is_default)
SELECT id, 'primary', 'rosado', '#FFD6E7', TRUE
FROM model_assets
WHERE asset_key = 'macaron_topping'
ON CONFLICT DO NOTHING;