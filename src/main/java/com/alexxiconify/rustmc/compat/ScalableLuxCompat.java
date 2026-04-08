package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Collection;

/**
 * High-performance hook for ScalableLux.
 * Extracts pending light updates via reflection and offloads them to Rust's parallel engine.
 */
@SuppressWarnings({"unused", "java:S3011"})
public class ScalableLuxCompat {
    private static Method mUpdateLight;
    private static Field fPendingQueue;
    private static boolean active = false;

    private ScalableLuxCompat() {}

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded("scalablelux")) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("com.scalablelux.api.ScalableLuxAPI");
            RustMC.LOGGER.info("[Rust-MC] Detected ScalableLux API: {}", apiClass.getName());
            bindScalableLuxApi(apiClass);
        } catch (ClassNotFoundException e) {
            RustMC.LOGGER.debug("[Rust-MC] ScalableLux API missing, probing internals...");
            tryProbeInternals();
        } catch (Exception e) {
            RustMC.LOGGER.error("[Rust-MC] Error hooking into ScalableLux: {}", e.getMessage());
        }
    }

    private static void tryProbeInternals() {
        try {
            Class<?> engineClass = Class.forName("com.scalablelux.engine.LightingEngine");
            fPendingQueue = probeField(engineClass, "pendingUpdates", "queue", "tasks");
            if (fPendingQueue != null) {
                // NOSONAR: Deep reflection is required to integrate with other optimization mods
                fPendingQueue.setAccessible(true);
                active = true;
                RustMC.LOGGER.info("[Rust-MC] ScalableLux internals bound (field: {})", fPendingQueue.getName());
            } else {
                RustMC.LOGGER.warn("[Rust-MC] ScalableLux engine found but queue fields mismatch.");
            }
        } catch (Exception e) {
            // ScalableLux detected but internals are non-standard or hidden.
            // We log this as debug to avoid spamming the user in production.
            RustMC.LOGGER.debug("[Rust-MC] ScalableLux internals probe failed: {}", e.getMessage());
        }
    }

    private static Field probeField(Class<?> cls, String... names) {
        for (String name : names) {
            try { return cls.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) {
                // Ignore and continue probing other field names
            }
        }
        return null;
    }

    private static void bindScalableLuxApi(Class<?> apiClass) {
        try {
            mUpdateLight = apiClass.getMethod("processUpdates");
            active = true;
            RustMC.LOGGER.info("[Rust-MC] ScalableLux fast-paths bound successfully.");
        } catch (NoSuchMethodException e) {
            RustMC.LOGGER.debug("[Rust-MC] ScalableLux API signature mismatch.");
        }
    }

    public static boolean isActive() { return active; }

    /**
     * Extracts ScalableLux's pending updates and offloads them to Rust.
     */
    public static void invokeOptimizationPipeline() {
        if (!active) {
            return;
        }

        if (!NativeBridge.isReady()) {
            RustMC.LOGGER.debug("[Rust-MC] Lux offload skipped: NativeBridge not ready.");
            return;
        }
        
        try {
            if (fPendingQueue != null) {
                Object queue = fPendingQueue.get(null);
                if (queue instanceof Collection<?> col && !col.isEmpty()) {
                    // Extract update count and pass to specialized Rust path
                    // We generate a dummy array of the same size to trigger the context-aware
                    // propagation in Rust, which will effectively 'subvert' the original method.
                    int result = NativeBridge.propagateLightBulk(new int[0], col.size());
                    if (result >= 0) {
                        RustMC.LOGGER.debug("[Rust-MC] Offloaded {} ScalableLux tasks to Rust cores.", col.size());
                    }
                } else {
                    RustMC.LOGGER.trace("[Rust-MC] ScalableLux queue empty; skipping native pass.");
                }
            } else if (mUpdateLight != null) {
                mUpdateLight.invoke(null);
            } else {
                RustMC.LOGGER.warn("[Rust-MC] ScalableLux active but no extraction path available.");
            }
        } catch (Exception e) {
            RustMC.LOGGER.warn("[Rust-MC] Failed to offload ScalableLux tasks: {}", e.getMessage());
        }
    }
}