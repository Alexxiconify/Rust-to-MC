package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;

// Integration with ImmediatelyFast (IF) — a rendering optimization mod that batches draw calls, defers HUD rendering, and optimizes text/map rendering.
@SuppressWarnings("unused") // Public API surface for mixins and future compat hooks
public final class ImmediatelyFastCompat {

    private ImmediatelyFastCompat() {}

    private static boolean initialized = false;
    private static boolean hudBatchingActive = false;
    private static boolean mapAtlasActive = false;
    private static boolean textOptActive = false;

    // Probes IF's configuration via reflection. Called once during mod init on a virtual thread.
    public static void initialize() {
        if (!ModBridge.IMMEDIATELYFAST || initialized) return;
        initialized = true;

        try {
            // IF exposes its config through net.raphimc.immediatelyfast.ImmediatelyFast
            Class<?> ifClass = Class.forName("net.raphimc.immediatelyfast.ImmediatelyFast");
            Object runtimeConfig = ifClass.getMethod("getRuntimeConfig").invoke(null);

            // Probe active optimizations
            hudBatchingActive = probeBoolean(runtimeConfig, "hud_batching", true);
            mapAtlasActive = probeBoolean(runtimeConfig, "map_atlas_generation", true);
            textOptActive = probeBoolean(runtimeConfig, "fast_text_lookup", true);

            RustMC.LOGGER.info("[Rust-MC] ImmediatelyFast detected: hudBatch={}, mapAtlas={}, textOpt={}",
                    hudBatchingActive, mapAtlasActive, textOptActive);

        } catch (ClassNotFoundException e) {
            // IF API class not found — older version, assume defaults
            hudBatchingActive = true;
            mapAtlasActive = true;
            textOptActive = true;
            RustMC.LOGGER.debug("[Rust-MC] IF detected but API not accessible ({}). Assuming defaults.", e.getMessage());
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] IF config probe failed ({}). Using safe defaults.", e.getMessage());
            hudBatchingActive = true;
            mapAtlasActive = true;
        }
    }

    @SuppressWarnings("java:S3400") // fallback parameter designed for future use with different defaults
    private static boolean probeBoolean(Object config, String fieldName, boolean fallback) {
        try {
            java.lang.reflect.Field f = config.getClass().getField(fieldName);
            return f.getBoolean(config);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ── Query API for other mixins ──────────────────────────────────────────

    // True when IF it is handling HUD draw call batching — we should skip our own HUD optimizations.
    public static boolean isHudBatchingActive() {
        return ModBridge.IMMEDIATELYFAST && hudBatchingActive;
    }

    // True when IF's map atlas is active — skip our own map rendering shortcuts.
    public static boolean isMapAtlasActive() {
        return ModBridge.IMMEDIATELYFAST && mapAtlasActive;
    }

    // True when IF it is optimizing text rendering — complement with native math, don't conflict.
    public static boolean isTextOptActive() {
        return ModBridge.IMMEDIATELYFAST && textOptActive;
    }

    // Returns a multiplier for particle/entity culling distance when IF is active. IF makes each draw call cheaper through batching, so we can afford more visible entities. <ul> <li>IF active + HUD batching: 1.3x (30% more entities visible)</li> <li>IF active without HUD batching: 1.15x</li> <li>IF not present: 1.0x (no change)</li> </ul>
    public static float getCullingDistanceMultiplier() {
        if (!ModBridge.IMMEDIATELYFAST) return 1.0f;
        return hudBatchingActive ? 1.3f : 1.15f;
    }
}