package com.alexxiconify.rustmc;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MixinManager implements IMixinConfigPlugin {

    private static final String PKG = "com.alexxiconify.rustmc.mixin.";

    // Fully-qualified mixin class names for precise matching (no false positives)
    private static final String MATH_MIXIN   = PKG + "MathHelperMixin";
    private static final String LIGHT_MIXIN  = PKG + "LightingMixin";
    private static final String PACKET_MIXIN = PKG + "PacketDeflaterMixin";
    private static final String DECODER_MIXIN= PKG + "DecoderHandlerMixin";
    private static final String PATH_MIXIN   = PKG + "PathfindingMixin";
    private static final String LOG_MIXIN    = PKG + "LoggingMixin";
    private static final String NOISE_MIXIN  = PKG + "SimplexNoiseSamplerMixin";
    private static final String CMD_MIXIN    = PKG + "CommandManagerMixin";

    @Override
    public void onLoad(String mixinPackage) {
        RustMC.LOGGER.info("[Rust-MC] MixinManager loaded for package: {}", mixinPackage);
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (LOG_MIXIN.equals(mixinClassName) || CMD_MIXIN.equals(mixinClassName))
            return true;
        // LoggingMixin is always applied – its body guards against early load via null checks
        if (LIGHT_MIXIN.equals(mixinClassName))
            return !ModBridge.isLightingOwned();

        if (MATH_MIXIN.equals(mixinClassName))
            return !ModBridge.isMathOwned();

        if (NOISE_MIXIN.equals(mixinClassName))
            return !ModBridge.isMathOwned(); // C2ME owns noise too

        if (PATH_MIXIN.equals(mixinClassName))
            return !ModBridge.isPathfindingOwned();

        if (PACKET_MIXIN.equals(mixinClassName) || DECODER_MIXIN.equals(mixinClassName))
            return !ModBridge.isNetworkingOwned();

        // Logging and commands: always apply unless specifically disabled
        // their bodies check NativeBridge.isReady() and config flags at runtime.
        return true;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        RustMC.LOGGER.debug("[Rust-MC] Mixins targets accepted: {}", myTargets);
    }
    @Override public List<String> getMixins() { return Collections.emptyList(); }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        RustMC.LOGGER.debug("[Rust-MC] Pre-applying {} to {}", mixinClassName, targetClassName);
    }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        RustMC.LOGGER.debug("[Rust-MC] Post-applied {} to {}", mixinClassName, targetClassName);
    }
}
