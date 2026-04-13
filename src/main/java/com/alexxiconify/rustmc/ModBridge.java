package com.alexxiconify.rustmc;

import net.fabricmc.loader.api.FabricLoader;

// Central mod detection registry. Fields marked as active are referenced by mixins, compat hooks, or ownership checks. Commented-out fields are detected but not yet wired into any optimization — uncomment when adding support.
public class ModBridge {
    // ── Core Performance Mods (actively used in ownership checks) ───────────
    public static final boolean SODIUM      = isMod("sodium");
    public static final boolean STARLIGHT   = isMod("starlight");
    public static final boolean C2ME        = isMod("c2me");
    public static final boolean IRIS        = isMod("iris");
    public static final boolean LITHIUM     = isMod("lithium");
    public static final boolean SCALABLELUX = isMod("scalablelux");
    public static final boolean MOONRISE    = isMod("moonrise");
    public static final boolean VMP         = isMod("vmp");
    public static final boolean FERRITECORE = isMod("ferritecore");
    public static final boolean MODERNFIX   = isMod("modernfix");
    public static final boolean SERVERCORE  = isMod("servercore");
    public static final boolean OXIDIZIUM   = isMod("oxidizium");
    public static final boolean MORECULLING = isMod("moreculling");
    // ── Networking Mods (used in isNetworkingOwned) ─────────────────────────
    public static final boolean RAKNETIFY     = isMod("raknetify");
    public static final boolean VIAFABRICPLUS = isMod("viafabricplus");
    public static final boolean PACKETFIXER   = isMod("packetfixer");
    public static final boolean AUTHME        = isMod("authme");
    // ── Compat Mods (actively used in mixin conditions / render hooks) ──────
    public static final boolean DISTANT_HORIZONS       = isMod("distanthorizons");
    public static final boolean BETTERBLOCKENTITIES    = isMod("betterblockentities");
    public static final boolean ENTITY_MODEL_FEATURES  = isMod("entity_model_features");
    public static final boolean ENTITY_TEXTURE_FEATURES = isMod("entity_texture_features");
    public static final boolean ENTITYCULLING          = isMod("entityculling");
    public static final boolean IMMEDIATELYFAST        = isMod("immediatelyfast");
    public static final boolean TICK_SYNC              = isMod("tick-sync");
    public static final boolean APPLESKIN              = isMod("appleskin");
    // ── HUD / Map Mods (actively used in render hooks) ──────────────────────
    public static final boolean MINIHUD      = isMod("minihud");
    public static final boolean LIGHTY       = isMod("lighty");
    public static final boolean XAEROPLUS    = isMod("xaeroplus");
    public static final boolean GNETUM       = isMod("gnetum");
    public static final boolean REESES_SODIUM = isMod("reeses-sodium-options");
    public static final boolean SODIUM_EXTRA = isMod("sodium-extra");
    // ── Interaction Mods (used in isInteractionOwned) ───────────────────────
    public static final boolean TWEAKEROO    = isMod("tweakeroo");
    public static final boolean LITEMATICA   = isMod("litematica");

    private ModBridge() {}

    private static boolean isMod(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    // ── Initialization ──────────────────────────────────────────────────────

    public static void initialize() {
        RustMC.LOGGER.info("[Rust-MC] Mod ecosystem detected: Sodium={}, Lithium={}, C2ME={}, Iris={}, " +
                        "ScalableLux={}, Ferritecore={}, Oxidizium={}, ImmediatelyFast={}, DH={}",
                SODIUM, LITHIUM, C2ME, IRIS, SCALABLELUX, FERRITECORE, OXIDIZIUM, IMMEDIATELYFAST, DISTANT_HORIZONS);
    }

    // ── Ownership Checks ────────────────────────────────────────────────────
    // Returns true if another mod (like Starlight or C2ME's lighting) owns the lighting threading model.
    public static boolean isLightingConflict() {
        return (STARLIGHT || (C2ME && !RustMC.CONFIG.isExperimentalCoexistEnabled())) && !RustMC.CONFIG.isExperimentalCoexistEnabled();
    }
    // Returns true if DH is present and handles its own high-performance lighting.
    public static boolean isDhLightingActive() {
        return DISTANT_HORIZONS;
    }
    // Returns true when C2ME or similar controls math/noise so we should skip our hooks.
    public static boolean isMathConflict() {
        return (C2ME || MOONRISE || MODERNFIX || FERRITECORE || SERVERCORE || LITHIUM) && RustMC.CONFIG.isBridgeC2ME();
    }
    // Returns true when Lithium or similar controls' pathfinding.
    public static boolean isPathfindingOwned() {
        return (LITHIUM || MOONRISE || VMP || SERVERCORE)
                && RustMC.CONFIG.isBridgeLithium();
    }
    // Returns true when a specialized networking mod controls packet flow.
    public static boolean isNetworkingConflict() {
        return RAKNETIFY || VIAFABRICPLUS || PACKETFIXER || AUTHME || SERVERCORE;
    }
    // Returns true when another mod's frustum would conflict with our Rust frustum.
    public static boolean isFrustumOwned() {
        return (SODIUM && !RustMC.CONFIG.isBridgeSodium()) || MORECULLING;
    }
    // Returns true if interaction/raycasting logic is handled by other mods.
    public static boolean isInteractionOwned() {
        return TWEAKEROO || LITEMATICA || MORECULLING;
    }
    // Gnetum owns HUD frame distribution — yield to it for HUD rendering.
    public static boolean isHudOwned() {
        return GNETUM;
    }
    // Reese's Sodium Options / Sodium Extra integrate visual configs in Sodium UI.
    public static boolean isVisualConfigIntegrated() {
        return REESES_SODIUM || SODIUM_EXTRA;
    }
}