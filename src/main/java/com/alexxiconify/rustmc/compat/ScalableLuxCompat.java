package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.RustMC;

import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Provides reflection-based integration with ScalableLux.
 * ScalableLux is a highly optimized lighting engine.
 * We disable our native lighting hooks when SL is present, but we can also
 * hook into their API to provide native chunk culling or data delivery if needed.
 */
public class ScalableLuxCompat {
    
    // Cache the methods to avoid reflection overhead on every frame/tick if we decide to wrap them.
    private static Method mUpdateLight;
    private static boolean active = false;

    private ScalableLuxCompat() {}

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded("scalablelux")) return;

        try {
            Class<?> apiClass = Class.forName("com.scalablelux.api.ScalableLuxAPI");
            RustMC.LOGGER.info("[Rust-MC] Detected ScalableLux API: {}", apiClass.getName());
            bindScalableLuxApi(apiClass);
        } catch (ClassNotFoundException e) {
            // SL is loaded but API is missing/obfuscated differently.
            RustMC.LOGGER.warn("[Rust-MC] ScalableLux detected but API not found. Disabling native lighting hooks anyway.");
        } catch (Exception e) {
            RustMC.LOGGER.error("[Rust-MC] Error hooking into ScalableLux: {}", e.getMessage());
        }
    }

    private static void bindScalableLuxApi(Class<?> apiClass) {
        // ScalableLux usually exposes query methods for light updates.
        // (These method names are hypothetical/representative of standard performance API wrappers).
        try {
            apiClass.getMethod("hasPendingUpdates"); // ensure it exists
            mUpdateLight = apiClass.getMethod("processUpdates");
            active = true;
            RustMC.LOGGER.info("[Rust-MC] ScalableLux fast-paths bound successfully.");
        } catch (NoSuchMethodException e) {
            RustMC.LOGGER.debug("[Rust-MC] ScalableLux API didn't match expected signatures; running in passive compat mode.");
        }
    }

    public static boolean isActive() {
        return active;
    }

    /**
     * Placeholder for the dynamic ScalableLux light integration.
     * If active, we would map their methods to native counterparts.
     */
    public static void invokeOptimizationPipeline(boolean forceImmediate) {
        if (active && mUpdateLight != null) {
            try {
                mUpdateLight.invoke(null, forceImmediate);
            } catch (Exception e) {
                RustMC.LOGGER.warn("[Rust-MC] Failed to invoke ScalableLux optimization hook: {}", e.getMessage());
            }
        }
    }
}
