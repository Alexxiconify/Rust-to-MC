package com.alexxiconify.rustmc;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Set;

public class MixinManager implements IMixinConfigPlugin {
    private static final boolean C2ME_PRESENT = isMod("c2me");
    private static final boolean VMP_PRESENT = isMod("vmp");
    private static final boolean SODIUM_PRESENT = isMod("sodium");
    private static final boolean IMMEDIATELYFAST_PRESENT = isMod("immediatelyfast");
    private static final boolean FERRITECORE_PRESENT = isMod("ferritecore");
    private static final boolean OXIDIZIUM_PRESENT = isMod("oxidizium");
    private static final boolean LITHIUM_PRESENT = isMod("lithium");
    private static final boolean STARLIGHT_PRESENT = isMod("starlight");
    private static final boolean KRYPTON_PRESENT = isMod("krypton");
    private static final boolean RAKNET_PRESENT = isMod("raknet");

    private static final String MATH_MIXIN = "MathHelperMixin";
    private static final String LIGHT_MIXIN = "LightingMixin";
    private static final String PACKET_MIXIN = "PacketDeflaterMixin";
    private static final String PATH_MIXIN = "PathfindingMixin";

    private static boolean isMod(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    @Override
    public void onLoad(String mixinPackage) {
        RustMC.LOGGER.info("Rust-to-MC Mixin Manager loaded.");
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(LIGHT_MIXIN)) return !SODIUM_PRESENT && !STARLIGHT_PRESENT && !C2ME_PRESENT;
        if (mixinClassName.contains(MATH_MIXIN)) return !OXIDIZIUM_PRESENT && !C2ME_PRESENT && !IMMEDIATELYFAST_PRESENT;
        if (mixinClassName.contains(PACKET_MIXIN)) return !KRYPTON_PRESENT && !RAKNET_PRESENT && !VMP_PRESENT;
        if (mixinClassName.contains(PATH_MIXIN)) return !LITHIUM_PRESENT;
        if (mixinClassName.contains("SimplexNoiseSamplerMixin")) return !FERRITECORE_PRESENT;
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
