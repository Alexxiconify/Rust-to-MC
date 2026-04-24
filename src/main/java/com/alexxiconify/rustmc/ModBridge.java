package com.alexxiconify.rustmc;
import net.fabricmc.loader.api.FabricLoader;
public class ModBridge {
    // Core performance mods used for ownership checks.
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

    // Networking mods used in ownership checks.
    public static final boolean RAKNETIFY     = isMod("raknetify");
    public static final boolean VIAFABRICPLUS = isMod("viafabricplus");
    public static final boolean PACKETFIXER   = isMod("packetfixer");
    public static final boolean AUTHME        = isMod("authme");

    // Compat mods used in mixin conditions and render hooks.
    public static final boolean DISTANT_HORIZONS       = isMod("distanthorizons");
    public static final boolean BETTERBLOCKENTITIES    = isMod("betterblockentities");
    public static final boolean ENTITY_MODEL_FEATURES  = isMod("entity_model_features");
    public static final boolean ENTITY_TEXTURE_FEATURES = isMod("entity_texture_features");
    public static final boolean ENTITYCULLING          = isMod("entityculling");
    public static final boolean IMMEDIATELYFAST        = isMod("immediatelyfast");
    public static final boolean TICK_SYNC              = isMod("tick-sync");
    public static final boolean APPLESKIN              = isMod("appleskin");

    // HUD/map mods used in overlay hooks.
    public static final boolean MINIHUD      = isMod("minihud");
    public static final boolean LIGHTY       = isMod("lighty");
    public static final boolean XAEROPLUS    = isMod("xaeroplus");
    public static final boolean GNETUM       = isMod("gnetum");
    public static final boolean REESES_SODIUM = isMod("reeses-sodium-options");
    public static final boolean SODIUM_EXTRA = isMod("sodium-extra");

    // Interaction mods used in ownership checks.
    public static final boolean TWEAKEROO    = isMod("tweakeroo");
    public static final boolean LITEMATICA   = isMod("litematica");

    private ModBridge() {}

    private static boolean isMod(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static void initialize() {
        RustMC.LOGGER.info("[Rust-MC] Mod ecosystem detected: Sodium={}, Lithium={}, C2ME={}, Iris={}, " +
                        "ScalableLux={}, Ferritecore={}, Oxidizium={}, ImmediatelyFast={}, DH={}",
                SODIUM, LITHIUM, C2ME, IRIS, SCALABLELUX, FERRITECORE, OXIDIZIUM, IMMEDIATELYFAST, DISTANT_HORIZONS);
    }

    // Returns true if another mod owns lighting threading and coexist mode is disabled.
    public static boolean isLightingOwned() {
        if (RustMC.CONFIG.isExperimentalCoexistEnabled()) {
            return false;
        }
        return STARLIGHT || SCALABLELUX;
    }

    public static boolean isDhLightingActive() {
        return DISTANT_HORIZONS;
    }

    public static boolean isMathOwned() {
        if (!RustMC.CONFIG.isBridgeC2ME()) {
            return false;
        }
        return C2ME || MOONRISE || MODERNFIX || FERRITECORE || SERVERCORE || LITHIUM;
    }

    public static boolean isPathfindingOwned() {
        return (LITHIUM || MOONRISE || VMP || SERVERCORE)
                && RustMC.CONFIG.isBridgeLithium();
    }

    public static boolean isNetworkingOwned() {
        return RAKNETIFY || VIAFABRICPLUS || PACKETFIXER || AUTHME || SERVERCORE;
    }

    public static boolean isFrustumOwned() {
        return (SODIUM && RustMC.CONFIG.isBridgeSodium()) || MORECULLING;
    }

    public static boolean isInteractionOwned() {
        return TWEAKEROO || LITEMATICA || MORECULLING;
    }

    public static boolean isHudOwned() {
        return GNETUM;
    }

    public static boolean isVisualConfigIntegrated() {
        return REESES_SODIUM || SODIUM_EXTRA;
    }
}