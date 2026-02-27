package com.alexxiconify.rustmc;

import net.fabricmc.loader.api.FabricLoader;

public class ModBridge {
    public static final boolean SODIUM = isMod("sodium");
    public static final boolean STARLIGHT = isMod("starlight");
    public static final boolean C2ME = isMod("c2me");
    public static final boolean IRIS = isMod("iris");
    public static final boolean LITHIUM = isMod("lithium");

    private static boolean isMod(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    private ModBridge() {}

    /**
     * Reconciles math requests between mods.
     * If multiple mods want to optimize math, we provide a unified native path.
     */
    public static double getFastMath(double value, String type) {
        if (NativeBridge.isReady()) {
            switch (type.toLowerCase()) {
                case "sin": return RustMC.CONFIG.isUseNativeSine() ? NativeBridge.invokeSin((float) value) : Math.sin(value);
                case "cos": return RustMC.CONFIG.isUseNativeCos() ? NativeBridge.invokeCos((float) value) : Math.cos(value);
                case "invsqrt": return RustMC.CONFIG.isUseNativeInvSqrt() ? NativeBridge.fastInvSqrt((float) value) : 1.0 / Math.sqrt(value);
                case "sqrt": return RustMC.CONFIG.isUseNativeSqrt() ? NativeBridge.invokeSqrt((float) value) : Math.sqrt(value);
                default: break;
            }
        }
        return Double.NaN;
    }

    public static boolean shouldBridgeLighting() {
        // If Starlight is present, we might want to bridge our optimizations into it
        // instead of just disabling ours entirely.
        return STARLIGHT || SODIUM;
    }
}
