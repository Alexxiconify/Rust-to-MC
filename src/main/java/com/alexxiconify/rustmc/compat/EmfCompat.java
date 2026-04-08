package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Optimizes Entity Model Features (EMF) by offloading vertex math to Rust.
 * EMF calculates many dynamic model parts per frame; we multi-thread these animations.
 */
public class EmfCompat {
    private static boolean active = false;

    private EmfCompat() {}

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded("entity_model_features")) {
            return;
        }
        active = true;
        RustMC.LOGGER.info("[Rust-MC] EMF Compatibility layer active – offloading animation math to Rust.");
    }

    public static boolean isActive() { return active; }

    /**
     * Called by mixins to offload model vertex transformations.
     */
    public static void optimizeModel(float[] vertices, float[] normals, float[] matrix) {
        if (!active) return;
        NativeBridge.transformVertices(vertices, normals, matrix);
    }
}
