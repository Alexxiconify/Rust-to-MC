package com.alexxiconify.rustmc;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class MixinManager implements IMixinConfigPlugin {

    private static final String PKG = "com.alexxiconify.rustmc.mixin.";

    /** Lookup table: mixin class name → condition that must be true for the mixin to apply. */
    private static final Map<String, BooleanSupplier> MIXIN_CONDITIONS;

    static {
     // Always-on

     MIXIN_CONDITIONS = Map.ofEntries (
       Map.entry ( PKG + "CommandManagerMixin" , ( ) -> true ) ,

       // Subsystem ownership guards
       Map.entry ( PKG + "LightingMixin" , ( ) -> !ModBridge.isLightingOwned ( ) ) , Map.entry ( PKG + "MathHelperMixin" , ( ) -> ModBridge.isMathOwned ( ) ) , Map.entry ( PKG + "SimplexNoiseSamplerMixin" , ( ) -> ModBridge.isMathOwned ( ) ) , Map.entry ( PKG + "PathfindingMixin" , ( ) -> !ModBridge.isPathfindingOwned ( ) ) , Map.entry ( PKG + "PacketDeflaterMixin" , ( ) -> ModBridge.isNetworkingOwned ( ) ) , Map.entry ( PKG + "DecoderHandlerMixin" , ( ) -> ModBridge.isNetworkingOwned ( ) ) ,

       // Config-gated
       Map.entry ( PKG + "BlockStateMixin" , RustMC.CONFIG :: isUseNativeCulling ) , Map.entry ( PKG + "ChunkBuilderMixin" , ( ) -> RustMC.CONFIG.isEnableChunkBuilderExpand ( ) && !ModBridge.SODIUM ) , Map.entry ( PKG + "compat.ClientRedstoneSkipMixin" , RustMC.CONFIG :: isEnableClientRedstoneSkip ) , Map.entry ( PKG + "compat.TickSyncCompatMixin" , RustMC.CONFIG :: isEnableTickSyncCompat ) , Map.entry ( PKG + "compat.BBECompatMixin" , RustMC.CONFIG :: isEnableBBECompat ) , Map.entry (
         PKG + "compat.EntityRenderCompatMixin" , ( ) -> RustMC.CONFIG.isEnableEMFCompat ( )
           || RustMC.CONFIG.isEnableETFCompat ( )
           || RustMC.CONFIG.isEnableEntityCullingCompat ( )
           || RustMC.CONFIG.isEnableImmediatelyFastCompat ( )
       ) ,

       // DNS cache mixins
       Map.entry ( PKG + "ServerPingerMixin" , RustMC.CONFIG :: isEnableDnsCache ) , Map.entry ( PKG + "ServerAddressMixin" , RustMC.CONFIG :: isEnableDnsCache ) , Map.entry ( PKG + "screen.MultiplayerScreenMixin" , RustMC.CONFIG :: isEnableDnsCache )
     );
    }

    @Override
    public void onLoad(String mixinPackage) {
        RustMC.LOGGER.info("[Rust-MC] MixinManager loaded for package: {}", mixinPackage);
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        BooleanSupplier condition = MIXIN_CONDITIONS.get(mixinClassName);
        if (condition != null) return condition.getAsBoolean();
        // Unknown mixin — apply by default (runtime checks guard actual behavior)
        return true;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { /* No-op: we don't need to filter targets */ }
    @Override public List<String> getMixins() { return Collections.emptyList(); }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { /* No-op: no pre-apply transforms needed */ }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { /* No-op: no post-apply transforms needed */ }
}