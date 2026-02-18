package com.alexxiconify.rustmc;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Set;

public class MixinManager implements IMixinConfigPlugin {
    private static final boolean OXIDIZIUM_PRESENT = FabricLoader.getInstance().isModLoaded("oxidizium");
    private static final boolean LITHIUM_PRESENT = FabricLoader.getInstance().isModLoaded("lithium");
    private static final boolean STARLIGHT_PRESENT = FabricLoader.getInstance().isModLoaded("starlight");
    private static final boolean KRYPTON_PRESENT = FabricLoader.getInstance().isModLoaded("krypton");
    private static final boolean RAKNET_PRESENT = FabricLoader.getInstance().isModLoaded("raknet");

    @Override
    public void onLoad(String mixinPackage) {
        RustMC.LOGGER.info("Rust-to-MC Mixin Manager loaded.");
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (OXIDIZIUM_PRESENT && mixinClassName.contains("MathHelperMixin")) return false;
        if (STARLIGHT_PRESENT && mixinClassName.contains("LightingMixin")) return false;
        if (KRYPTON_PRESENT && mixinClassName.contains("PacketDeflaterMixin")) return false;
        if (RAKNET_PRESENT && mixinClassName.contains("PacketDeflaterMixin")) return false;
        
        return !(LITHIUM_PRESENT && mixinClassName.contains("PathfindingMixin"));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No additional target processing required
    }
    @Override
    public List<String> getMixins() { return null; }
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No custom logic before applying mixins
    }
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No custom logic after applying mixins
    }
}
