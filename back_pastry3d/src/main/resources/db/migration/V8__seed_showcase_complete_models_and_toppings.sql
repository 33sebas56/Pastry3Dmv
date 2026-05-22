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
    'flan_napolitano_complete',
    'Flan napolitano completo',
    'COMPLETE_DESSERT',
    'flan',
    '/uploads/models/demo/flan_napolitano_complete.glb',
    'LOCAL_ASSET',
    'READY',
    0.98,
    'flan,flan napolitano,napolitano,caramelo,caramel,custard,dessert,postre completo,complete',
    'FIXED',
    '{}'
),
(
    'chocolate_cake_complete',
    'Pastel de chocolate completo',
    'COMPLETE_DESSERT',
    'chocolate_cake',
    '/uploads/models/demo/chocolate_cake_complete.glb',
    'LOCAL_ASSET',
    'READY',
    0.97,
    'pastel chocolate,torta chocolate,chocolate cake,cake,pastel,torta,birthday cake,complete,postre completo',
    'FIXED',
    '{}'
),
(
    'strawberry_cake_complete',
    'Pastel de fresa completo',
    'COMPLETE_DESSERT',
    'strawberry_cake',
    '/uploads/models/demo/strawberry_cake_complete.glb',
    'LOCAL_ASSET',
    'READY',
    0.97,
    'pastel fresa,torta fresa,strawberry cake,cake,pastel,torta,fresas,strawberry,complete,postre completo',
    'FIXED',
    '{}'
),
(
    'donut_glazed_complete',
    'Dona glaseada completa',
    'COMPLETE_DESSERT',
    'donut',
    '/uploads/models/demo/donut_glazed_complete.glb',
    'LOCAL_ASSET',
    'READY',
    0.94,
    'donut,dona,rosquilla,glazed,glaseada,postre completo,complete',
    'FIXED',
    '{}'
),
(
    'ice_cream_cup_complete',
    'Copa de helado completa',
    'COMPLETE_DESSERT',
    'ice_cream',
    '/uploads/models/demo/ice_cream_cup_complete.glb',
    'LOCAL_ASSET',
    'READY',
    0.94,
    'helado,copa de helado,ice cream,ice cream cup,dessert,postre completo,complete',
    'FIXED',
    '{}'
),
(
    'blueberry_topping',
    'Arándanos decorativos',
    'TOPPING',
    NULL,
    '/uploads/models/demo/blueberry_topping.glb',
    'LOCAL_ASSET',
    'READY',
    0.86,
    'blueberry,blueberries,arandano,arándano,arandanos,arándanos,berry,berries,fruta,topping,decoracion,decoration',
    'MULTI_MATERIAL',
    '{"primary":{"recolorable":true,"default":"#2F3E8C"},"fruit":{"recolorable":true,"default":"#2F3E8C"}}'
),
(
    'cherry_topping',
    'Cereza decorativa',
    'TOPPING',
    NULL,
    '/uploads/models/demo/cherry_topping.glb',
    'LOCAL_ASSET',
    'READY',
    0.86,
    'cherry,cereza,cerezas,red fruit,fruta roja,topping,decoracion,decoration',
    'MULTI_MATERIAL',
    '{"primary":{"recolorable":true,"default":"#C9182B"},"fruit":{"recolorable":true,"default":"#C9182B"}}'
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
SELECT id, 'primary', 'azul arándano', '#2F3E8C', TRUE
FROM model_assets
WHERE asset_key = 'blueberry_topping'
ON CONFLICT DO NOTHING;

INSERT INTO model_color_variants (model_asset_id, material_name, color_name, color_hex, is_default)
SELECT id, 'primary', 'rojo cereza', '#C9182B', TRUE
FROM model_assets
WHERE asset_key = 'cherry_topping'
ON CONFLICT DO NOTHING;