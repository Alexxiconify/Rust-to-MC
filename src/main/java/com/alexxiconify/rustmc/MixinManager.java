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
    private static final String NOISE_MIXIN  = PKG + "SimplexNoiseSamplerMixin";
    private static final String CMD_MIXIN    = PKG + "CommandManagerMixin";
    private static final String BLOCK_MIXIN  = PKG + "BlockStateMixin";
    private static final String CHUNK_BUILDER_MIXIN = PKG + "ChunkBuilderMixin";
    private static final String REDSTONE_MIXIN = PKG + "compat.ClientRedstoneSkipMixin";
    private static final String TICK_SYNC_MIXIN = PKG + "compat.TickSyncCompatMixin";
    private static final String BBE_MIXIN    = PKG + "compat.BBECompatMixin";
    private static final String ENTITY_RENDER_MIXIN = PKG + "compat.EntityRenderCompatMixin";
    private static final String SERVER_PINGER_MIXIN = PKG + "ServerPingerMixin";
    private static final String SERVER_ADDRESS_MIXIN = PKG + "ServerAddressMixin";
    private static final String MULTIPLAYER_SCREEN_MIXIN = PKG + "screen.MultiplayerScreenMixin";

    @Override
    public void onLoad(String mixinPackage) {
        RustMC.LOGGER.info("[Rust-MC] MixinManager loaded for package: {}", mixinPackage);
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (CMD_MIXIN.equals(mixinClassName))
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

        if (BLOCK_MIXIN.equals(mixinClassName))
            return RustMC.CONFIG.isUseNativeCulling();

        if (CHUNK_BUILDER_MIXIN.equals(mixinClassName))
            return RustMC.CONFIG.isEnableChunkBuilderExpand() && !ModBridge.SODIUM;

        if (REDSTONE_MIXIN.equals(mixinClassName))
            return RustMC.CONFIG.isEnableClientRedstoneSkip();

        if (TICK_SYNC_MIXIN.equals(mixinClassName))
            return RustMC.CONFIG.isEnableTickSyncCompat();

        if (BBE_MIXIN.equals(mixinClassName))
            return RustMC.CONFIG.isEnableBBECompat();

        if (ENTITY_RENDER_MIXIN.equals(mixinClassName))
            return RustMC.CONFIG.isEnableEMFCompat() || RustMC.CONFIG.isEnableETFCompat()
                    || RustMC.CONFIG.isEnableEntityCullingCompat();

        // DNS cache mixins — respect the enableDnsCache config toggle
        if (SERVER_PINGER_MIXIN.equals(mixinClassName)
                || SERVER_ADDRESS_MIXIN.equals(mixinClassName)
                || MULTIPLAYER_SCREEN_MIXIN.equals(mixinClassName))
            return RustMC.CONFIG.isEnableDnsCache();

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