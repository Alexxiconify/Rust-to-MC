package com.alexxiconify.rustmc;

import net.fabricmc.loader.api.FabricLoader;

// Caches mod-detection results at startup to avoid repeated lookups during gameplay.
// All flags are initialized once and remain constant for the session.
// This replaces repeated calls to ModBridge.isXXX() in hot paths, eliminating
// the cost of mod detection during render loops.
public final class ModBridgeCache {
    private static volatile boolean initialized = false;

    // Cached mod detection results
    private static boolean hasSodium;
    private static boolean hasStarlight;
    private static boolean hasC2ME;
    private static boolean hasIris;
    private static boolean hasLithium;
    private static boolean hasScalableLux;
    private static boolean hasImmediatelyFast;
    private static boolean hasDistantHorizons;
    private static boolean hasEntityModelFeatures;
    private static boolean hasEntityTextureFeatures;

    // Initialize cache once at startup. Safe to call multiple times (idempotent).
    public static synchronized void initialize() {
        if (initialized) return;

        try {
            hasSodium = ModBridge.SODIUM;
            hasStarlight = ModBridge.STARLIGHT;
            hasC2ME = ModBridge.C2ME;
            hasIris = ModBridge.IRIS;
            hasLithium = ModBridge.LITHIUM;
            hasScalableLux = ModBridge.SCALABLELUX;
            hasImmediatelyFast = ModBridge.IMMEDIATELYFAST;

            // Safe loader queries with null checks
            FabricLoader loader = FabricLoader.getInstance();
            if (loader != null) {
                hasDistantHorizons = loader.isModLoaded("distanthorizons");
                hasEntityModelFeatures = loader.isModLoaded("entity_model_features");
                hasEntityTextureFeatures = loader.isModLoaded("entity_texture_features");
            }

            initialized = true;
            RustMC.LOGGER.debug("[Rust-MC] ModBridgeCache initialized: sodium={}, starlight={}, c2me={}, immediately_fast={}, dh={}",
                    hasSodium, hasStarlight, hasC2ME, hasImmediatelyFast, hasDistantHorizons);
        } catch (Exception e) {
            RustMC.LOGGER.warn("[Rust-MC] ModBridgeCache initialization failed: {}", e.getMessage());
            initialized = true; // Mark as initialized even on error to avoid retry spam
        }
    }

    // Getter methods for cached values
    public static boolean isSodiumLoaded() { return hasSodium; }
    public static boolean isStarlightLoaded() { return hasStarlight; }
    public static boolean isC2MELoaded() { return hasC2ME; }
    public static boolean isIrisLoaded() { return hasIris; }
    public static boolean isLithiumLoaded() { return hasLithium; }
    public static boolean isScalableLuxLoaded() { return hasScalableLux; }
    public static boolean isImmediatelyFastLoaded() { return hasImmediatelyFast; }
    public static boolean isDistantHorizonsLoaded() { return hasDistantHorizons; }
    public static boolean isEntityModelFeaturesLoaded() { return hasEntityModelFeatures; }
    public static boolean isEntityTextureFeaturesLoaded() { return hasEntityTextureFeatures; }

    private ModBridgeCache() {
        // Static utility class
    }
}