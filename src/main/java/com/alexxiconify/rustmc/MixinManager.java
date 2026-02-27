package com.alexxiconify.rustmc;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinManager implements IMixinConfigPlugin {
    private static final String MATH_MIXIN    = "MathHelperMixin";
    private static final String LIGHT_MIXIN   = "LightingMixin";
    private static final String PACKET_MIXIN  = "PacketDeflaterMixin";
    private static final String PATH_MIXIN    = "PathfindingMixin";
    private static final String LOG_MIXIN     = "LoggingMixin";
    private static final String NOISE_MIXIN   = "SimplexNoiseSamplerMixin";
    private static final String CMD_MIXIN     = "CommandManagerMixin";

    @Override
    public void onLoad(String mixinPackage) {
        RustMC.LOGGER.info("[Rust-MC] MixinManager loaded for package: {}", mixinPackage);
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // LoggingMixin: safe to apply – the mixin itself guards against early-load via null check
        if (mixinClassName.contains(LOG_MIXIN)) return true;

        // Lighting: skip when another mod already owns lighting
        if (mixinClassName.contains(LIGHT_MIXIN))
            return !ModBridge.SODIUM && !ModBridge.STARLIGHT && !ModBridge.C2ME;

        // Math: skip when C2ME owns math
        if (mixinClassName.contains(MATH_MIXIN))
            return !ModBridge.C2ME;

        // Noise: skip when C2ME owns world-gen
        if (mixinClassName.contains(NOISE_MIXIN))
            return !ModBridge.C2ME;

        // Pathfinding: skip when Lithium owns pathfinding
        if (mixinClassName.contains(PATH_MIXIN))
            return !ModBridge.LITHIUM;

        // Packet / Command: always apply; runtime config guards inside the mixin
        if (mixinClassName.contains(PACKET_MIXIN)) return true;
        if (mixinClassName.contains(CMD_MIXIN))    return true;

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return java.util.Collections.emptyList(); }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
