package com.backend.pastry3d.shared.constants;

public final class AppConstants {
    private AppConstants() {}

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final String STATUS_READY = "READY";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PENDING_MANUAL_MODEL = "PENDING_MANUAL_MODEL";
    public static final String STATUS_GENERATING = "GENERATING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_IMPORTED = "IMPORTED";
    public static final String STATUS_REUSED = "REUSED";

    public static final String CATEGORY_COMPLETE_DESSERT = "COMPLETE_DESSERT";
    public static final String CATEGORY_BASE = "BASE";
    public static final String CATEGORY_TOPPING = "TOPPING";
    public static final String CATEGORY_DECORATION = "DECORATION";
    public static final String CATEGORY_SAUCE = "SAUCE";
    public static final String CATEGORY_SPRINKLES = "SPRINKLES";

    public static final String STRATEGY_REUSE_COMPLETE_MODEL = "REUSE_COMPLETE_MODEL";
    public static final String STRATEGY_COMPOSE_SCENE = "COMPOSE_SCENE";
    public static final String STRATEGY_PENDING_MANUAL_MODEL = "PENDING_MANUAL_MODEL";
    public static final String STRATEGY_GENERATE_NEW_MODEL = "GENERATE_NEW_MODEL";
}
