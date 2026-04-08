package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Compatibility layer for Particle Rain mod.
 * Offloads weather particle movement and collision logic to Rust.
 */
public class ParticleRainCompat {
    private static boolean active = false;

    private ParticleRainCompat() {}

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded("particlerain")) {
            return;
        }
        active = true;
        RustMC.LOGGER.info("[Rust-MC] Particle Rain detected – enabling native weather particle optimization.");
    }

    public static boolean isActive() { return active; }

    /**
     * Offloads the complex weather particle physics to Rust.
     * We pass positions and velocities for bulk processing.
     */
    public static void optimizePhysics(double[] positions, double[] velocities) {
        if (active && NativeBridge.isReady()) {
            NativeBridge.tickParticles(positions, velocities, 0.05); // Rain gravity constant
        }
    }
}
