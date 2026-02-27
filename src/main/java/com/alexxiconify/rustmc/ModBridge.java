package com.alexxiconify.rustmc;

import net.fabricmc.loader.api.FabricLoader;

public class ModBridge {
    public static final boolean SODIUM    = isMod("sodium");
    public static final boolean STARLIGHT = isMod("starlight");
    public static final boolean C2ME      = isMod("c2me");
    public static final boolean IRIS      = isMod("iris");
    public static final boolean LITHIUM   = isMod("lithium");
    public static final boolean INDIUM    = isMod("indium"); // Sodium compat layer
    public static final boolean SCALABLELUX = isMod("scalablelux");
    public static final boolean MOONRISE  = isMod("moonrise");
    public static final boolean VMP       = isMod("vmp");
    public static final boolean FERRITECORE = isMod("ferritecore");
    public static final boolean RAKNETIFY = isMod("raknetify");
    public static final boolean MODERNFIX = isMod("modernfix");
    public static final boolean BADOPTIMIZATIONS = isMod("badoptimizations");
    public static final boolean VIAFABRICPLUS = isMod("viafabricplus");
    public static final boolean LIGHTY    = isMod("lighty");

    private static boolean isMod(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    private ModBridge() {}

    /**
     * Returns true if a rendering/lighting override mod is present.
     * When true, the native lighting mixin should be disabled so the
     * other mod can own lighting without interference.
     */
    public static boolean isLightingOwned() {
        return STARLIGHT || SCALABLELUX || MOONRISE || LIGHTY || (SODIUM && RustMC.CONFIG.isBridgeSodium())
                || C2ME || (IRIS && RustMC.CONFIG.isBridgeIris()) || BADOPTIMIZATIONS;
    }

    /**
     * Returns true when C2ME controls math/noise so we should skip our hooks.
     */
    public static boolean isMathOwned() {
        return (C2ME || MOONRISE || MODERNFIX || FERRITECORE) && RustMC.CONFIG.isBridgeC2ME();
    }

    /**
     * Returns true when Lithium controls pathfinding so we should skip our hook.
     */
    public static boolean isPathfindingOwned() {
        return (LITHIUM || MOONRISE || VMP) && RustMC.CONFIG.isBridgeLithium();
    }

    /**
     * Unified fast-math dispatch: tries Rust native, falls back to Java.
     */
    public static double getFastMath(double value, String type) {
        if (!NativeBridge.isReady()) return Double.NaN;
        return switch (type.toLowerCase()) {
            case "sin"     -> RustMC.CONFIG.isUseNativeSine()    ? NativeBridge.invokeSin((float) value)    : (float) Math.sin(value);
            case "cos"     -> RustMC.CONFIG.isUseNativeCos()     ? NativeBridge.invokeCos((float) value)    : (float) Math.cos(value);
            case "invsqrt" -> RustMC.CONFIG.isUseNativeInvSqrt() ? NativeBridge.fastInvSqrt((float) value)  : 1.0 / Math.sqrt(value);
            case "sqrt"    -> RustMC.CONFIG.isUseNativeSqrt()    ? NativeBridge.invokeSqrt((float) value)   : (float) Math.sqrt(value);
            default        -> Double.NaN;
        };
    }
}
