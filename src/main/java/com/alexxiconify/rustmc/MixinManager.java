package com.alexxiconify.rustmc;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinManager implements IMixinConfigPlugin {
    private static final String MATH_MIXIN = "MathHelperMixin";
    private static final String LIGHT_MIXIN = "LightingMixin";
    private static final String PACKET_MIXIN = "PacketDeflaterMixin";
    private static final String PATH_MIXIN = "PathfindingMixin";
    private static final String LOG_MIXIN = "LoggingMixin";
    private static final String NOISE_MIXIN = "SimplexNoiseSamplerMixin";

    @Override
    public void onLoad(String mixinPackage) {
        RustMC.LOGGER.info("Rust-to-MC Mixin Manager loaded.");
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(LOG_MIXIN)) return false; // Temporarily disabled due to early load crash
        
        if (mixinClassName.contains(LIGHT_MIXIN)) return !ModBridge.SODIUM && !ModBridge.STARLIGHT && !ModBridge.C2ME;
        if (mixinClassName.contains(MATH_MIXIN)) return !ModBridge.C2ME; 
        if (mixinClassName.contains(PACKET_MIXIN)) return true;
        if (mixinClassName.contains(PATH_MIXIN)) return !ModBridge.LITHIUM;
        if (mixinClassName.contains(NOISE_MIXIN)) return !ModBridge.C2ME;
        
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No additional target processing required
    }
    @Override
    public List<String> getMixins() { return java.util.Collections.emptyList(); }
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No custom logic before applying mixins
    }
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No custom logic after applying mixins
    }
}
